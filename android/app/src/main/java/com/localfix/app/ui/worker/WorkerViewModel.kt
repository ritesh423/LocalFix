package com.localfix.app.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.worker.WorkerData
import com.localfix.app.data.worker.WorkerJob
import com.localfix.app.data.worker.WorkerRepository
import com.localfix.app.data.worker.WorkerSyncState
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestStatusTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkerViewModel(
    private val repository: WorkerRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(WorkerSelection())

    val uiState = combine(
        repository.workerData,
        repository.syncState,
        selection,
        ::createWorkerUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createWorkerUiState(
            repository.workerData.value,
            repository.syncState.value,
            selection.value,
        ),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun openJob(ticketId: String) {
        selection.value = WorkerSelection(ticketId = ticketId)
    }

    fun startJob() {
        val job = uiState.value.detail.job ?: return
        if (!job.canStart || uiState.value.detail.isStarting) return

        selection.update { current ->
            current.copy(isStarting = true, startError = null, hasJustStarted = false)
        }
        viewModelScope.launch {
            runCatching {
                repository.startJob(
                    ticketId = job.id,
                    expectedVersion = job.version,
                )
            }.onSuccess {
                selection.update { current ->
                    current.copy(isStarting = false, hasJustStarted = true)
                }
            }.onFailure {
                selection.update { current ->
                    current.copy(
                        isStarting = false,
                        startError = "Couldn't start this job. Refresh your queue and try again.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: WorkerRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { WorkerViewModel(repository) }
            }
    }
}

private data class WorkerSelection(
    val ticketId: String? = null,
    val isStarting: Boolean = false,
    val startError: String? = null,
    val hasJustStarted: Boolean = false,
)

private fun createWorkerUiState(
    data: WorkerData,
    syncState: WorkerSyncState,
    selection: WorkerSelection,
): WorkerUiState {
    val selectedJob = data.jobs.find { it.id == selection.ticketId }
    return WorkerUiState(
        queue = WorkerQueueUiState(
            workerName = data.workerName,
            propertyName = data.propertyName,
            readyCount = data.jobs.count { it.status == TicketStatus.ASSIGNED },
            inProgressCount = data.jobs.count { it.status == TicketStatus.IN_PROGRESS },
            jobs = data.jobs.map(WorkerJob::toQueueItem),
            loadState = syncState.toLoadUiState(data.jobs.isNotEmpty()),
        ),
        detail = WorkerJobDetailUiState(
            job = selectedJob?.toDetail(),
            isStarting = selection.isStarting,
            startError = selection.startError,
            hasJustStarted = selection.hasJustStarted,
        ),
    )
}

private fun WorkerSyncState.toLoadUiState(hasJobs: Boolean): RequestLoadUiState = when (this) {
    WorkerSyncState.InitialLoading -> RequestLoadUiState.Loading
    WorkerSyncState.Refreshing -> RequestLoadUiState.Refreshing
    WorkerSyncState.Ready -> if (hasJobs) {
        RequestLoadUiState.Content
    } else {
        RequestLoadUiState.Empty
    }
    is WorkerSyncState.Error -> if (hasPreviousResult) {
        RequestLoadUiState.Stale(message)
    } else {
        RequestLoadUiState.Failed(message)
    }
}

private fun WorkerJob.toQueueItem() = WorkerJobItem(
    id = id,
    reference = reference,
    unitLabel = unitLabel,
    title = title,
    categoryLabel = category.label,
    priorityLabel = priorityLabel,
    accessWindowLabel = accessWindow.label,
    statusLabel = status.label,
    statusTone = status.tone,
    updatedLabel = updatedLabel,
)

private fun WorkerJob.toDetail() = WorkerJobDetail(
    id = id,
    reference = reference,
    unitLabel = unitLabel,
    title = title,
    description = description,
    categoryLabel = category.label,
    residentUrgencyLabel = urgencySuggestion.label,
    priorityLabel = priorityLabel,
    accessWindowLabel = accessWindow.label,
    statusLabel = status.label,
    statusTone = status.tone,
    version = version,
    canStart = status == TicketStatus.ASSIGNED,
)

private val TicketStatus.label: String
    get() = when (this) {
        TicketStatus.OPEN -> "Open"
        TicketStatus.ASSIGNED -> "Ready to start"
        TicketStatus.IN_PROGRESS -> "In progress"
        TicketStatus.BLOCKED -> "Blocked"
        TicketStatus.AWAITING_CONFIRMATION -> "Awaiting confirmation"
        TicketStatus.COMPLETED -> "Completed"
        TicketStatus.CANCELLED -> "Cancelled"
    }

private val TicketStatus.tone: RequestStatusTone
    get() = when (this) {
        TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS -> RequestStatusTone.ACTIVE
        TicketStatus.BLOCKED, TicketStatus.AWAITING_CONFIRMATION ->
            RequestStatusTone.ATTENTION
        TicketStatus.COMPLETED -> RequestStatusTone.COMPLETED
        TicketStatus.OPEN, TicketStatus.CANCELLED -> RequestStatusTone.NEUTRAL
    }

private val UrgencySuggestion.label: String
    get() = when (this) {
        UrgencySuggestion.ROUTINE -> "Routine"
        UrgencySuggestion.SOON -> "Soon"
        UrgencySuggestion.URGENT -> "Urgent"
    }

private val AccessWindow.label: String
    get() = when (this) {
        AccessWindow.ANYTIME -> "Any time today"
        AccessWindow.MORNING -> "Morning · 8 AM–12 PM"
        AccessWindow.AFTERNOON -> "Afternoon · 12–4 PM"
        AccessWindow.EVENING -> "Evening · 4–8 PM"
    }
