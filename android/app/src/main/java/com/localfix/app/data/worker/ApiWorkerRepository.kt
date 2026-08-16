package com.localfix.app.data.worker

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketCompletionPayload
import com.localfix.app.data.remote.TicketStartPayload
import com.localfix.app.data.remote.WorkerTicketApi
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApiWorkerRepository(
    private val ticketApi: WorkerTicketApi,
    private val clock: Clock = Clock.systemUTC(),
) : WorkerRepository {
    private val mutableWorkerData = MutableStateFlow(emptyWorkerData())
    private val mutableSyncState =
        MutableStateFlow<WorkerSyncState>(WorkerSyncState.InitialLoading)
    private var hasLoaded = false

    override val workerData: StateFlow<WorkerData> = mutableWorkerData.asStateFlow()
    override val syncState: StateFlow<WorkerSyncState> = mutableSyncState.asStateFlow()

    override suspend fun refresh() {
        mutableSyncState.value = if (hasLoaded) {
            WorkerSyncState.Refreshing
        } else {
            WorkerSyncState.InitialLoading
        }
        runCatching { ticketApi.listWorkerTickets() }
            .onSuccess { tickets ->
                mutableWorkerData.update { data ->
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

    override suspend fun startJob(
        ticketId: String,
        expectedVersion: Int,
    ): WorkerJob {
        val started = ticketApi.startTicket(
            ticketId = ticketId,
            request = TicketStartPayload(expectedVersion),
        ).toWorkerJob(clock)
        mutableWorkerData.update { data ->
            data.copy(
                jobs = data.jobs.map { existing ->
                    if (existing.id == started.id) started else existing
                },
            )
        }
        return started
    }

    override suspend fun submitCompletion(
        ticketId: String,
        expectedVersion: Int,
        completionNote: String,
        partsUsed: List<String>,
        photoUri: String,
    ): WorkerJob {
        val completed = ticketApi.submitCompletion(
            ticketId = ticketId,
            request = TicketCompletionPayload(
                expectedVersion = expectedVersion,
                completionNote = completionNote,
                partsUsed = partsUsed,
                photoUri = photoUri,
            ),
        ).toWorkerJob(clock)
        mutableWorkerData.update { data ->
            data.copy(
                jobs = data.jobs.map { existing ->
                    if (existing.id == completed.id) completed else existing
                },
            )
        }
        return completed
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
    updatedLabel = updatedAt.toUpdatedLabel(clock),
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
