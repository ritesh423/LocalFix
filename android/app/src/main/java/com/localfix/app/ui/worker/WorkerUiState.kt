package com.localfix.app.ui.worker

import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestStatusTone

data class WorkerUiState(
    val queue: WorkerQueueUiState,
    val detail: WorkerJobDetailUiState,
)

data class WorkerQueueUiState(
    val workerName: String,
    val propertyName: String,
    val readyCount: Int,
    val inProgressCount: Int,
    val jobs: List<WorkerJobItem>,
    val loadState: RequestLoadUiState,
)

data class WorkerJobItem(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val categoryLabel: String,
    val priorityLabel: String,
    val accessWindowLabel: String,
    val statusLabel: String,
    val statusTone: RequestStatusTone,
    val updatedLabel: String,
)

data class WorkerJobDetailUiState(
    val job: WorkerJobDetail? = null,
    val isStarting: Boolean = false,
    val startError: String? = null,
    val hasJustStarted: Boolean = false,
    val completionDraft: WorkerCompletionDraft = WorkerCompletionDraft(),
    val completionErrors: WorkerCompletionErrors = WorkerCompletionErrors(),
    val isSubmittingCompletion: Boolean = false,
    val completionSubmissionError: String? = null,
    val hasJustSubmittedCompletion: Boolean = false,
    val history: List<WorkerHistoryItem> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val historyError: String? = null,
)

data class WorkerHistoryItem(
    val id: String,
    val title: String,
    val detail: String?,
    val statusLabel: String,
    val timeLabel: String,
    val ticketVersion: Int,
)

data class WorkerCompletionDraft(
    val completionNote: String = "",
    val partsUsed: String = "",
    val photoUri: String? = null,
)

data class WorkerCompletionErrors(
    val completionNote: String? = null,
    val partsUsed: String? = null,
    val photo: String? = null,
)

data class WorkerJobDetail(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val residentUrgencyLabel: String,
    val priorityLabel: String,
    val accessWindowLabel: String,
    val statusLabel: String,
    val statusTone: RequestStatusTone,
    val version: Int,
    val canStart: Boolean,
    val canSubmitCompletion: Boolean,
    val completionNote: String?,
    val partsUsed: List<String>,
    val hasCompletionPhoto: Boolean,
    val reworkReason: String?,
)
