package com.localfix.app.data.model

data class SavedRequestDraft(
    val clientRequestId: String,
    val category: ServiceCategory?,
    val title: String,
    val description: String,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val photoUri: String? = null,
)
