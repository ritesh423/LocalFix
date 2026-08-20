package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.localfix.app.data.model.RequestDeliveryState
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingResidentReviewDao {
    @Query("SELECT * FROM pending_resident_reviews ORDER BY queuedAt DESC")
    fun observeReviews(): Flow<List<PendingResidentReviewEntity>>

    @Query("SELECT * FROM pending_resident_reviews WHERE ticketId = :ticketId")
    suspend fun getReview(ticketId: String): PendingResidentReviewEntity?

    @Query("SELECT ticketId FROM pending_resident_reviews WHERE deliveryState = 'PENDING'")
    suspend fun getRetryableReviewIds(): List<String>

    @Upsert
    suspend fun upsertReview(review: PendingResidentReviewEntity)

    @Query(
        "UPDATE pending_resident_reviews " +
            "SET deliveryState = :deliveryState, failureMessage = :message " +
            "WHERE ticketId = :ticketId",
    )
    suspend fun updateDeliveryState(
        ticketId: String,
        deliveryState: RequestDeliveryState,
        message: String?,
    )
}
