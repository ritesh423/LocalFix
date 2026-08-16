package com.localfix.app.data.worker

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SampleWorkerRepository : WorkerRepository {
    private val mutableData = MutableStateFlow(sampleWorkerData())

    override val workerData: StateFlow<WorkerData> = mutableData.asStateFlow()
    override val syncState = MutableStateFlow<WorkerSyncState>(WorkerSyncState.Ready)

    override suspend fun refresh() = Unit

    override suspend fun startJob(ticketId: String, expectedVersion: Int): WorkerJob {
        val current = requireNotNull(mutableData.value.jobs.find { it.id == ticketId })
        require(current.version == expectedVersion)
        require(current.status == TicketStatus.ASSIGNED)
        val started = current.copy(
            status = TicketStatus.IN_PROGRESS,
            version = current.version + 1,
            updatedLabel = "Updated just now",
        )
        mutableData.update { data ->
            data.copy(
                jobs = data.jobs.map { job ->
                    if (job.id == started.id) started else job
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
        val current = requireNotNull(mutableData.value.jobs.find { it.id == ticketId })
        require(current.version == expectedVersion)
        require(current.status == TicketStatus.IN_PROGRESS)
        require(photoUri.isNotBlank())
        val completed = current.copy(
            status = TicketStatus.AWAITING_CONFIRMATION,
            version = current.version + 1,
            completionNote = completionNote,
            partsUsed = partsUsed,
            hasCompletionPhoto = true,
            updatedLabel = "Updated just now",
        )
        mutableData.update { data ->
            data.copy(jobs = data.jobs.map { if (it.id == ticketId) completed else it })
        }
        return completed
    }
}

fun sampleWorkerData() = WorkerData(
    workerName = "Arun Kumar",
    propertyName = "Lakeview Residency",
    jobs = listOf(
        WorkerJob(
            id = "90000000-0000-0000-0000-000000000001",
            reference = "LF-90000000",
            unitLabel = "Apartment A-204",
            title = "Bathroom pipe is leaking",
            description = "Water is collecting below the bathroom washbasin pipe.",
            category = ServiceCategory.PLUMBING,
            urgencySuggestion = UrgencySuggestion.SOON,
            priorityLabel = "Soon",
            accessWindow = AccessWindow.MORNING,
            status = TicketStatus.ASSIGNED,
            version = 2,
            completionNote = null,
            partsUsed = emptyList(),
            hasCompletionPhoto = false,
            updatedLabel = "Updated 12 min ago",
        ),
        WorkerJob(
            id = "90000000-0000-0000-0000-000000000002",
            reference = "LF-90000001",
            unitLabel = "Apartment C-305",
            title = "Kitchen sink drain is blocked",
            description = "Water drains very slowly and collects in the kitchen sink.",
            category = ServiceCategory.PLUMBING,
            urgencySuggestion = UrgencySuggestion.ROUTINE,
            priorityLabel = "Routine",
            accessWindow = AccessWindow.AFTERNOON,
            status = TicketStatus.IN_PROGRESS,
            version = 3,
            completionNote = null,
            partsUsed = emptyList(),
            hasCompletionPhoto = false,
            updatedLabel = "Updated 45 min ago",
        ),
    ),
)
