package com.localfix.app.ui.requestdetail

import com.localfix.app.data.resident.ResidentReviewDecision
import com.localfix.app.ui.requests.RequestStatusTone

data class ResidentRequestDetailUiState(
    val requestId: String,
    val id: String,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val statusLabel: String,
    val statusTone: RequestStatusTone,
    val urgencyLabel: String,
    val accessWindowLabel: String,
    val assignedWorker: String,
    val updatedLabel: String,
    val photoUri: String?,
    val completionNote: String?,
    val partsUsed: List<String>,
    val completionPhotoUrl: String?,
    val residentRating: Int?,
    val residentFeedback: String?,
    val canReview: Boolean,
    val review: ResidentReviewUiState,
)

data class ResidentReviewUiState(
    val selectedDecision: ResidentReviewDecision? = null,
    val rating: Int? = null,
    val feedback: String = "",
    val decisionError: String? = null,
    val ratingError: String? = null,
    val feedbackError: String? = null,
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val hasJustSubmitted: Boolean = false,
)
