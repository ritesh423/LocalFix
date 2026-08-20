package com.localfix.app.ui.requestdetail

import com.localfix.app.data.model.ResidentReviewDecision
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
    val delivery: RequestDeliveryUiState?,
    val reviewDelivery: ReviewDeliveryUiState? = null,
    val review: ResidentReviewUiState,
)

data class RequestDeliveryUiState(
    val title: String,
    val message: String,
    val canRetry: Boolean,
    val canDiscard: Boolean,
    val isWorking: Boolean = false,
    val actionError: String? = null,
)

data class ReviewDeliveryUiState(
    val title: String,
    val message: String,
    val isFailure: Boolean,
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
