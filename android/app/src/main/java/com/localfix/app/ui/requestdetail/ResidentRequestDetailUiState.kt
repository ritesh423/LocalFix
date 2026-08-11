package com.localfix.app.ui.requestdetail

import com.localfix.app.ui.requests.RequestStatusTone

data class ResidentRequestDetailUiState(
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
)
