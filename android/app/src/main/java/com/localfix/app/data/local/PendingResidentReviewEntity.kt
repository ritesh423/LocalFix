package com.localfix.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ResidentReviewDecision

@Entity(tableName = "pending_resident_reviews")
data class PendingResidentReviewEntity(
    @PrimaryKey val ticketId: String,
    val expectedVersion: Int,
    val decision: ResidentReviewDecision,
    val rating: Int?,
    val feedback: String?,
    val deliveryState: RequestDeliveryState,
    val failureMessage: String?,
    val queuedAt: String,
)
