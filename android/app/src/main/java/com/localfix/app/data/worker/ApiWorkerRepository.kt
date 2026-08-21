package com.localfix.app.data.worker

import com.localfix.app.data.command.TicketCommandStore
import com.localfix.app.data.command.TicketCommandSyncScheduler
import com.localfix.app.data.local.PendingTicketCommandEntity
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketEventResponse
import com.localfix.app.data.remote.WorkerTicketApi
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ApiWorkerRepository(
    private val ticketApi: WorkerTicketApi,
    private val commandStore: TicketCommandStore,
    private val commandSyncScheduler: TicketCommandSyncScheduler,
    applicationScope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) : WorkerRepository {
    private val serverData = MutableStateFlow(emptyWorkerData())
    private val mutableSyncState =
        MutableStateFlow<WorkerSyncState>(WorkerSyncState.InitialLoading)
    private var hasLoaded = false

    override val workerData: StateFlow<WorkerData> = combine(
        serverData,
        commandStore.observeCommands(),
    ) { data, commands -> data.withPendingCommands(commands) }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyWorkerData())
    override val syncState: StateFlow<WorkerSyncState> = mutableSyncState.asStateFlow()

    override suspend fun refresh() {
        mutableSyncState.value = if (hasLoaded) {
            WorkerSyncState.Refreshing
        } else {
            WorkerSyncState.InitialLoading
        }
        runCatching { ticketApi.listWorkerTickets() }
            .onSuccess { tickets ->
                serverData.update { data ->
                    data.copy(jobs = tickets.map { it.toWorkerJob(clock) })
                }
                hasLoaded = true
                mutableSyncState.value = WorkerSyncState.Ready
            }
            .onFailure {
                mutableSyncState.value = WorkerSyncState.Error(
                    message = CONNECTION_ERROR,
                    hasPreviousResult = hasLoaded,
                )
            }
    }

    override suspend fun loadJobHistory(ticketId: String): List<WorkerJobEvent> =
        ticketApi.listWorkerTicketEvents(ticketId).map { it.toWorkerJobEvent(clock) }

    override suspend fun startJob(
        ticketId: String,
        expectedVersion: Int,
    ): WorkerJob {
        val command = PendingTicketCommandEntity(
            ticketId = ticketId,
            commandType = TicketCommandType.START,
            expectedVersion = expectedVersion,
            priority = null,
            workerId = null,
            deliveryState = RequestDeliveryState.PENDING,
            failureMessage = null,
            queuedAt = clock.instant().toString(),
        )
        commandStore.queue(command)
        runCatching {
            commandSyncScheduler.schedule(
                ticketId,
                TicketCommandType.START,
                replaceExisting = true,
            )
        }.onFailure {
            commandStore.markFailed(
                ticketId,
                TicketCommandType.START,
                "Starting this job was saved, but its retry could not be scheduled.",
            )
        }.getOrThrow()
        return requireNotNull(serverData.value.jobs.find { it.id == ticketId }).copy(
            startDeliveryState = RequestDeliveryState.PENDING,
        )
    }

    fun acceptSyncedTicket(ticket: TicketResponse) {
        val started = ticket.toWorkerJob(clock)
        serverData.update { data ->
            data.copy(
                jobs = data.jobs.map { existing ->
                    if (existing.id == started.id) started else existing
                },
            )
        }
    }

    override suspend fun submitCompletion(
        ticketId: String,
        expectedVersion: Int,
        completionNote: String,
        partsUsed: List<String>,
        photoUri: String,
    ): WorkerJob {
        val command = PendingTicketCommandEntity(
            ticketId = ticketId,
            commandType = TicketCommandType.COMPLETE,
            expectedVersion = expectedVersion,
            priority = null,
            workerId = null,
            completionNote = completionNote,
            partsUsed = partsUsed,
            photoUri = photoUri,
            deliveryState = RequestDeliveryState.PENDING,
            failureMessage = null,
            queuedAt = clock.instant().toString(),
        )
        commandStore.queue(command)
        runCatching {
            commandSyncScheduler.schedule(
                ticketId,
                TicketCommandType.COMPLETE,
                replaceExisting = true,
            )
        }.onFailure {
            commandStore.markFailed(
                ticketId,
                TicketCommandType.COMPLETE,
                "The repair was saved, but its upload could not be scheduled.",
            )
        }.getOrThrow()
        return requireNotNull(serverData.value.jobs.find { it.id == ticketId }).copy(
            completionDeliveryState = RequestDeliveryState.PENDING,
            pendingCompletionNote = completionNote,
            pendingPartsUsed = partsUsed,
            pendingPhotoUri = photoUri,
        )
    }

    private companion object {
        const val CONNECTION_ERROR =
            "We couldn't reach LocalFix. Check your connection and try again."

        fun emptyWorkerData() = WorkerData(
            workerName = "Arun Kumar",
            propertyName = "Lakeview Residency",
            jobs = emptyList(),
        )
    }
}

private fun WorkerData.withPendingCommands(
    commands: List<PendingTicketCommandEntity>,
): WorkerData {
    val starts = commands
        .filter { it.commandType == TicketCommandType.START }
        .associateBy(PendingTicketCommandEntity::ticketId)
    val completions = commands
        .filter { it.commandType == TicketCommandType.COMPLETE }
        .associateBy(PendingTicketCommandEntity::ticketId)
    return copy(jobs = jobs.map { job ->
        val start = starts[job.id]
        val completion = completions[job.id]
        job.copy(
            startDeliveryState = start?.deliveryState ?: RequestDeliveryState.SYNCED,
            startFailureMessage = start?.failureMessage,
            completionDeliveryState = completion?.deliveryState
                ?: RequestDeliveryState.SYNCED,
            completionFailureMessage = completion?.failureMessage,
            pendingCompletionNote = completion?.completionNote,
            pendingPartsUsed = completion?.partsUsed.orEmpty(),
            pendingPhotoUri = completion?.photoUri,
        )
    })
}

private fun TicketResponse.toWorkerJob(clock: Clock): WorkerJob = WorkerJob(
    id = id,
    reference = if (id.startsWith("LF-")) id else "LF-${id.take(8).uppercase()}",
    unitLabel = unitId.toUnitLabel(),
    title = title,
    description = description,
    category = ServiceCategory.valueOf(category.uppercase()),
    urgencySuggestion = UrgencySuggestion.valueOf(urgencySuggestion.uppercase()),
    priorityLabel = priority?.replaceFirstChar(Char::uppercase) ?: "Not set",
    accessWindow = AccessWindow.valueOf(accessWindow.uppercase()),
    status = TicketStatus.valueOf(status.uppercase()),
    version = version,
    completionNote = completionNote,
    partsUsed = partsUsed,
    hasCompletionPhoto = hasCompletionPhoto,
    reworkReason = residentFeedback,
    updatedLabel = updatedAt.toUpdatedLabel(clock),
)

private fun TicketEventResponse.toWorkerJobEvent(clock: Clock) = WorkerJobEvent(
    id = id,
    title = action.toEventTitle(),
    detail = detail,
    statusLabel = toStatus.toStatusLabel(),
    timeLabel = createdAt.toUpdatedLabel(clock),
    ticketVersion = ticketVersion,
)

private fun String.toEventTitle(): String = when (this) {
    "create" -> "Request created"
    "assign" -> "Worker assigned"
    "start" -> "Work started"
    "submit_proof" -> "Repair submitted"
    "confirm" -> "Repair confirmed"
    "request_rework" -> "Resident requested more work"
    "history_started" -> "History tracking started"
    else -> replace('_', ' ').replaceFirstChar(Char::uppercase)
}

private fun String.toStatusLabel(): String = when (this) {
    "open" -> "Open"
    "assigned" -> "Ready to start"
    "in_progress" -> "In progress"
    "awaiting_confirmation" -> "Awaiting confirmation"
    "completed" -> "Completed"
    "blocked" -> "Blocked"
    "cancelled" -> "Cancelled"
    else -> replace('_', ' ').replaceFirstChar(Char::uppercase)
}

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
