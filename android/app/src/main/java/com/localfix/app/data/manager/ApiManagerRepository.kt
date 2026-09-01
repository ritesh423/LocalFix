package com.localfix.app.data.manager

import com.localfix.app.data.command.TicketCommandStore
import com.localfix.app.data.command.TicketCommandSyncScheduler
import com.localfix.app.data.local.PendingTicketCommandEntity
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.ManagerTicketApi
import com.localfix.app.data.remote.ManagerSummaryResponse
import com.localfix.app.data.remote.ManagerPropertyUnitResponse
import com.localfix.app.data.remote.ManagerResidentInviteCreatePayload
import com.localfix.app.data.remote.ManagerResidentInviteResponse
import com.localfix.app.data.remote.ManagerWorkerInviteCreatePayload
import com.localfix.app.data.remote.ManagerWorkerInviteResponse
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.WorkerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ApiManagerRepository(
    private val ticketApi: ManagerTicketApi,
    private val commandStore: TicketCommandStore,
    private val commandSyncScheduler: TicketCommandSyncScheduler,
    applicationScope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) : ManagerRepository {
    private val serverData = MutableStateFlow(emptyManagerData())
    private val mutableSyncState =
        MutableStateFlow<ManagerSyncState>(ManagerSyncState.InitialLoading)
    private var hasLoaded = false

    override val managerData: StateFlow<ManagerData> = combine(
        serverData,
        commandStore.observeCommands(),
    ) { data, commands -> data.withPendingAssignments(commands) }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyManagerData())
    override val syncState: StateFlow<ManagerSyncState> = mutableSyncState.asStateFlow()

    override suspend fun refresh() {
        mutableSyncState.value = if (hasLoaded) {
            ManagerSyncState.Refreshing
        } else {
            ManagerSyncState.InitialLoading
        }
        runCatching {
            ManagerRefreshResponse(
                tickets = ticketApi.listManagerTickets(),
                workers = ticketApi.listManagerWorkers(),
                summary = ticketApi.getManagerSummary(),
                units = ticketApi.listManagerUnits(),
            )
        }.onSuccess { response ->
            val unitsById = response.units.associateBy(ManagerPropertyUnitResponse::id)
            serverData.update { data ->
                data.copy(
                    tickets = response.tickets.map { ticket ->
                        ticket.toManagerTicket(
                            clock = clock,
                            unitLabel = unitsById[ticket.unitId]?.label,
                        )
                    },
                    workers = response.workers.map(WorkerResponse::toManagerWorker),
                    summary = response.summary.toManagerSummary(),
                    units = response.units.map(ManagerPropertyUnitResponse::toDomain),
                )
            }
            hasLoaded = true
            mutableSyncState.value = ManagerSyncState.Ready
        }.onFailure {
            mutableSyncState.value = ManagerSyncState.Error(
                message = CONNECTION_ERROR,
                hasPreviousResult = hasLoaded,
            )
        }
    }

    override suspend fun createResidentInvite(
        unitId: String,
        validDays: Int,
    ): ManagerResidentInvite = ticketApi.createManagerResidentInvite(
        ManagerResidentInviteCreatePayload(unitId, validDays),
    ).toDomain()

    override suspend fun createWorkerInvite(
        name: String,
        specialty: ServiceCategory,
        validDays: Int,
    ): ManagerWorkerInvite {
        val invite = ticketApi.createManagerWorkerInvite(
            ManagerWorkerInviteCreatePayload(
                name = name,
                specialty = specialty.name.lowercase(),
                validDays = validDays,
            ),
        ).toDomain()
        serverData.update { data ->
            data.copy(
                workers = (data.workers + invite.worker).distinctBy(ManagerWorker::id),
            )
        }
        return invite
    }

    override suspend fun assignTicket(
        ticketId: String,
        expectedVersion: Int,
        priority: ManagerPriority,
        workerId: String,
    ): ManagerTicket {
        val command = PendingTicketCommandEntity(
            ticketId = ticketId,
            commandType = TicketCommandType.ASSIGN,
            expectedVersion = expectedVersion,
            priority = priority.name.lowercase(),
            workerId = workerId,
            deliveryState = RequestDeliveryState.PENDING,
            failureMessage = null,
            queuedAt = clock.instant().toString(),
        )
        commandStore.queue(command)
        runCatching {
            commandSyncScheduler.schedule(
                ticketId = ticketId,
                commandType = TicketCommandType.ASSIGN,
                replaceExisting = true,
            )
        }.onFailure {
            commandStore.markFailed(
                ticketId,
                TicketCommandType.ASSIGN,
                "This assignment is saved, but its retry could not be scheduled.",
            )
        }.getOrThrow()
        val current = requireNotNull(serverData.value.tickets.find { it.id == ticketId })
        return current.copy(
            priority = priority,
            assignedWorkerId = workerId,
            assignedWorker = serverData.value.workers.find { it.id == workerId }?.name,
            commandDeliveryState = RequestDeliveryState.PENDING,
        )
    }

    fun acceptSyncedTicket(ticket: TicketResponse) {
        val assigned = ticket.toManagerTicket(clock)
        serverData.update { data ->
            val previous = data.tickets.find { it.id == assigned.id }
            data.copy(
                tickets = data.tickets.map { existing ->
                    if (existing.id == assigned.id) assigned else existing
                },
                summary = if (
                    previous?.status == TicketStatus.OPEN &&
                    assigned.status == TicketStatus.ASSIGNED
                ) {
                    data.summary.copy(
                        needsAssignment = (data.summary.needsAssignment - 1)
                            .coerceAtLeast(0),
                        assigned = data.summary.assigned + 1,
                    )
                } else {
                    data.summary
                },
            )
        }
    }

    private companion object {
        const val CONNECTION_ERROR =
            "We couldn't reach LocalFix. Check your connection and try again."

        fun emptyManagerData() = ManagerData(
            propertyName = "Lakeview Residency",
            tickets = emptyList(),
            workers = emptyList(),
            units = emptyList(),
            summary = ManagerSummary.Empty,
        )
    }
}

private data class ManagerRefreshResponse(
    val tickets: List<TicketResponse>,
    val workers: List<WorkerResponse>,
    val summary: ManagerSummaryResponse,
    val units: List<ManagerPropertyUnitResponse>,
)

private fun ManagerData.withPendingAssignments(
    commands: List<PendingTicketCommandEntity>,
): ManagerData {
    val assignments = commands
        .filter { it.commandType == TicketCommandType.ASSIGN }
        .associateBy(PendingTicketCommandEntity::ticketId)
    return copy(
        tickets = tickets.map { ticket ->
            val command = assignments[ticket.id] ?: return@map ticket
            ticket.copy(
                priority = command.priority?.let { ManagerPriority.valueOf(it.uppercase()) },
                assignedWorkerId = command.workerId,
                assignedWorker = workers.find { it.id == command.workerId }?.name,
                commandDeliveryState = command.deliveryState,
                commandFailureMessage = command.failureMessage,
            )
        },
    )
}

private fun TicketResponse.toManagerTicket(
    clock: Clock,
    unitLabel: String? = null,
): ManagerTicket = ManagerTicket(
    id = id,
    reference = if (id.startsWith("LF-")) id else "LF-${id.take(8).uppercase()}",
    unitLabel = unitLabel ?: unitId.toUnitLabel(),
    title = title,
    description = description,
    category = ServiceCategory.valueOf(category.uppercase()),
    urgencySuggestion = UrgencySuggestion.valueOf(urgencySuggestion.uppercase()),
    priority = priority?.let { ManagerPriority.valueOf(it.uppercase()) },
    accessWindow = AccessWindow.valueOf(accessWindow.uppercase()),
    status = TicketStatus.valueOf(status.uppercase()),
    version = version,
    assignedWorkerId = assignedWorkerId,
    assignedWorker = assignedWorker,
    updatedLabel = updatedAt.toUpdatedLabel(clock),
)

private fun WorkerResponse.toManagerWorker(): ManagerWorker = ManagerWorker(
    id = id,
    name = name,
    specialty = ServiceCategory.valueOf(specialty.uppercase()),
)

private fun ManagerPropertyUnitResponse.toDomain() = ManagerPropertyUnit(
    id = id,
    label = label,
)

private fun ManagerResidentInviteResponse.toDomain() = ManagerResidentInvite(
    inviteCode = inviteCode,
    unitId = unitId,
    unitLabel = unitLabel,
    expiresAt = expiresAt,
)

private fun ManagerWorkerInviteResponse.toDomain() = ManagerWorkerInvite(
    inviteCode = inviteCode,
    worker = worker.toManagerWorker(),
    expiresAt = expiresAt,
)

private fun ManagerSummaryResponse.toManagerSummary() = ManagerSummary(
    totalRequests = totalRequests,
    activeRequests = activeRequests,
    needsAssignment = needsAssignment,
    assigned = assigned,
    inProgress = inProgress,
    blocked = blocked,
    awaitingConfirmation = awaitingConfirmation,
    completed = completed,
)

private fun String.toUnitLabel(): String = when (this) {
    "30000000-0000-0000-0000-000000000204" -> "Apartment A-204"
    else -> "Apartment ${takeLast(4).uppercase()}"
}

private fun String.toUpdatedLabel(clock: Clock): String {
    val elapsed = runCatching {
        Duration.between(Instant.parse(this), clock.instant()).coerceAtLeast(Duration.ZERO)
    }.getOrDefault(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "Updated just now"
        elapsed.toHours() < 1 -> "Updated ${elapsed.toMinutes()} min ago"
        elapsed.toDays() < 1 -> "Updated ${elapsed.toHours()} hr ago"
        else -> "Updated ${elapsed.toDays()} days ago"
    }
}
