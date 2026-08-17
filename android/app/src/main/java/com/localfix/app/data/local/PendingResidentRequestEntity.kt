package com.localfix.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion

@Entity(tableName = "pending_resident_requests")
data class PendingResidentRequestEntity(
    @PrimaryKey val clientRequestId: String,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val photoUri: String?,
    val deliveryState: RequestDeliveryState,
    val failureMessage: String?,
    val queuedAt: String,
)
