package com.localfix.app.data.manager

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.ManagerTicketApi
import com.localfix.app.data.remote.TicketAssignmentPayload
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.WorkerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ApiManagerRepository(
    private val ticketApi: ManagerTicketApi,
    private val clock: Clock = Clock.systemUTC(),
) : ManagerRepository {
    private val mutableManagerData = MutableStateFlow(emptyManagerData())
    private val mutableSyncState =
        MutableStateFlow<ManagerSyncState>(ManagerSyncState.InitialLoading)
    private var hasLoaded = false

    override val managerData: StateFlow<ManagerData> = mutableManagerData.asStateFlow()
    override val syncState: StateFlow<ManagerSyncState> = mutableSyncState.asStateFlow()

    override suspend fun refresh() {
        mutableSyncState.value = if (hasLoaded) {
            ManagerSyncState.Refreshing
        } else {
            ManagerSyncState.InitialLoading
        }
        runCatching {
            ticketApi.listManagerTickets() to ticketApi.listManagerWorkers()
        }.onSuccess { (tickets, workers) ->
            mutableManagerData.update { data ->
                data.copy(
                    tickets = tickets.map { it.toManagerTicket(clock) },
                    workers = workers.map(WorkerResponse::toManagerWorker),
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

    override suspend fun assignTicket(
        ticketId: String,
        expectedVersion: Int,
        priority: ManagerPriority,
        workerId: String,
    ): ManagerTicket {
        val assigned = ticketApi.assignTicket(
            ticketId = ticketId,
            request = TicketAssignmentPayload(
                expectedVersion = expectedVersion,
                priority = priority.name.lowercase(),
                workerId = workerId,
            ),
        ).toManagerTicket(clock)
        mutableManagerData.update { data ->
            data.copy(
                tickets = data.tickets.map { existing ->
                    if (existing.id == assigned.id) assigned else existing
                },
            )
        }
        return assigned
    }

    private companion object {
        const val CONNECTION_ERROR =
            "We couldn't reach LocalFix. Check your connection and try again."

        fun emptyManagerData() = ManagerData(
            propertyName = "Lakeview Residency",
            tickets = emptyList(),
            workers = emptyList(),
        )
    }
}

private fun TicketResponse.toManagerTicket(clock: Clock): ManagerTicket = ManagerTicket(
    id = id,
    reference = if (id.startsWith("LF-")) id else "LF-${id.take(8).uppercase()}",
    unitLabel = unitId.toUnitLabel(),
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
