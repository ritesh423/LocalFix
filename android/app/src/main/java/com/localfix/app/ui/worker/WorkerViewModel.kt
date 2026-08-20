package com.localfix.app.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.worker.WorkerData
import com.localfix.app.data.worker.WorkerJob
import com.localfix.app.data.worker.WorkerJobEvent
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
        val job = repository.workerData.value.jobs.find { it.id == ticketId }
        selection.value = WorkerSelection(
            ticketId = ticketId,
            isHistoryLoading = true,
            startError = job?.startFailureMessage,
        )
        viewModelScope.launch { loadJobHistory(ticketId) }
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
            }.onSuccess { startedJob ->
                val wasQueued = startedJob.startDeliveryState == RequestDeliveryState.PENDING
                selection.update { current ->
                    current.copy(isStarting = false, hasJustStarted = !wasQueued)
                }
                if (!wasQueued) loadJobHistory(job.id)
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

    fun updateCompletionNote(value: String) {
        selection.update { current ->
            current.copy(
                completionNote = value.take(500),
                completionNoteError = null,
                completionSubmissionError = null,
            )
        }
    }

    fun updatePartsUsed(value: String) {
        selection.update { current ->
            current.copy(
                partsUsed = value.take(500),
                partsUsedError = null,
                completionSubmissionError = null,
            )
        }
    }

    fun updateCompletionPhoto(photoUri: String) {
        selection.update { current ->
            current.copy(
                photoUri = photoUri,
                photoError = null,
                completionSubmissionError = null,
            )
        }
    }

    fun removeCompletionPhoto() {
        selection.update { current -> current.copy(photoUri = null, photoError = null) }
    }

    fun reportCompletionPhotoFailure() {
        selection.update { current ->
            current.copy(photoError = "That photo couldn't be kept. Choose another image.")
        }
    }

    fun submitCompletion() {
        val detail = uiState.value.detail
        val job = detail.job ?: return
        if (!job.canSubmitCompletion || detail.isSubmittingCompletion) return
        val current = selection.value
        val noteError = if (current.completionNote.trim().length < 10) {
            "Describe the completed repair in at least 10 characters"
        } else {
            null
        }
        val parts = current.partsUsed.toPartsList()
        val partsError = if (parts.size > 10 || parts.any { it.length > 80 }) {
            "Use at most 10 comma-separated parts, each under 80 characters"
        } else {
            null
        }
        val photoError = if (current.photoUri == null) {
            "Add an after-repair photo"
        } else {
            null
        }
        if (noteError != null || partsError != null || photoError != null) {
            selection.update {
                it.copy(
                    completionNoteError = noteError,
                    partsUsedError = partsError,
                    photoError = photoError,
                )
            }
            return
        }

        selection.update {
            it.copy(isSubmittingCompletion = true, completionSubmissionError = null)
        }
        viewModelScope.launch {
            runCatching {
                repository.submitCompletion(
                    ticketId = job.id,
                    expectedVersion = job.version,
                    completionNote = current.completionNote.trim(),
                    partsUsed = parts,
                    photoUri = requireNotNull(current.photoUri),
                )
            }.onSuccess {
                selection.update { state ->
                    state.copy(
                        isSubmittingCompletion = false,
                        hasJustSubmittedCompletion = true,
                    )
                }
                loadJobHistory(job.id)
            }.onFailure {
                selection.update { state ->
                    state.copy(
                        isSubmittingCompletion = false,
                        completionSubmissionError =
                            "Couldn't submit this repair. Check the photo and try again.",
                    )
                }
            }
        }
    }

    private suspend fun loadJobHistory(ticketId: String) {
        runCatching { repository.loadJobHistory(ticketId) }
            .onSuccess { events ->
                selection.update { current ->
                    if (current.ticketId == ticketId) {
                        current.copy(
                            history = events,
                            isHistoryLoading = false,
                            historyError = null,
                        )
                    } else {
                        current
                    }
                }
            }
            .onFailure {
                selection.update { current ->
                    if (current.ticketId == ticketId) {
                        current.copy(
                            isHistoryLoading = false,
                            historyError = "Couldn't load this job's activity.",
                        )
                    } else {
                        current
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
    val completionNote: String = "",
    val partsUsed: String = "",
    val photoUri: String? = null,
    val completionNoteError: String? = null,
    val partsUsedError: String? = null,
    val photoError: String? = null,
    val isSubmittingCompletion: Boolean = false,
    val completionSubmissionError: String? = null,
    val hasJustSubmittedCompletion: Boolean = false,
    val history: List<WorkerJobEvent> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val historyError: String? = null,
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
            readyCount = data.jobs.count {
                it.status == TicketStatus.ASSIGNED &&
                    it.startDeliveryState != RequestDeliveryState.PENDING
            },
            inProgressCount = data.jobs.count { it.status == TicketStatus.IN_PROGRESS },
            jobs = data.jobs.map(WorkerJob::toQueueItem),
            loadState = syncState.toLoadUiState(data.jobs.isNotEmpty()),
        ),
        detail = WorkerJobDetailUiState(
            job = selectedJob?.toDetail(),
            isStarting = selection.isStarting,
            startError = selection.startError,
            hasJustStarted = selection.hasJustStarted,
            completionDraft = WorkerCompletionDraft(
                completionNote = selection.completionNote,
                partsUsed = selection.partsUsed,
                photoUri = selection.photoUri,
            ),
            completionErrors = WorkerCompletionErrors(
                completionNote = selection.completionNoteError,
                partsUsed = selection.partsUsedError,
                photo = selection.photoError,
            ),
            isSubmittingCompletion = selection.isSubmittingCompletion,
            completionSubmissionError = selection.completionSubmissionError,
            hasJustSubmittedCompletion = selection.hasJustSubmittedCompletion,
            history = selection.history.map(WorkerJobEvent::toUiState),
            isHistoryLoading = selection.isHistoryLoading,
            historyError = selection.historyError,
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
    statusLabel = startStatusLabel,
    statusTone = startStatusTone,
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
    statusLabel = startStatusLabel,
    statusTone = startStatusTone,
    version = version,
    canStart = status == TicketStatus.ASSIGNED &&
        startDeliveryState != RequestDeliveryState.PENDING,
    canSubmitCompletion = status == TicketStatus.IN_PROGRESS &&
        startDeliveryState == RequestDeliveryState.SYNCED,
    completionNote = completionNote,
    partsUsed = partsUsed,
    hasCompletionPhoto = hasCompletionPhoto,
    reworkReason = reworkReason,
)

private val WorkerJob.startStatusLabel: String
    get() = when (startDeliveryState) {
        RequestDeliveryState.PENDING -> "Waiting to start"
        RequestDeliveryState.FAILED -> "Start failed"
        RequestDeliveryState.SYNCED -> status.label
    }

private val WorkerJob.startStatusTone: RequestStatusTone
    get() = when (startDeliveryState) {
        RequestDeliveryState.PENDING -> RequestStatusTone.NEUTRAL
        RequestDeliveryState.FAILED -> RequestStatusTone.ATTENTION
        RequestDeliveryState.SYNCED -> status.tone
    }

private fun WorkerJobEvent.toUiState() = WorkerHistoryItem(
    id = id,
    title = title,
    detail = detail,
    statusLabel = statusLabel,
    timeLabel = timeLabel,
    ticketVersion = ticketVersion,
)

private fun String.toPartsList(): List<String> = split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)

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
