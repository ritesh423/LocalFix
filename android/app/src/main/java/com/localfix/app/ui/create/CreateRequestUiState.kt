package com.localfix.app.ui.create

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion

data class CreateRequestUiState(
    val draft: RequestDraft = RequestDraft(),
    val errors: RequestDraftErrors = RequestDraftErrors(),
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val submittedRequestId: String? = null,
)

data class RequestDraft(
    val category: ServiceCategory? = null,
    val title: String = "",
    val description: String = "",
    val urgencySuggestion: UrgencySuggestion = UrgencySuggestion.ROUTINE,
    val accessWindow: AccessWindow = AccessWindow.ANYTIME,
)

data class RequestDraftErrors(
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
) {
    val hasErrors: Boolean
        get() = category != null || title != null || description != null
}
