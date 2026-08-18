package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.localfix.app.data.model.RequestDeliveryState
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingResidentRequestDao {
    @Query("SELECT * FROM pending_resident_requests ORDER BY queuedAt DESC")
    fun observeRequests(): Flow<List<PendingResidentRequestEntity>>

    @Query("SELECT * FROM pending_resident_requests WHERE clientRequestId = :clientRequestId")
    suspend fun getRequest(clientRequestId: String): PendingResidentRequestEntity?

    @Query("SELECT COUNT(*) FROM pending_resident_requests")
    suspend fun countRequests(): Int

    @Query(
        "SELECT clientRequestId FROM pending_resident_requests " +
            "WHERE deliveryState = 'PENDING'",
    )
    suspend fun getRetryableRequestIds(): List<String>

    @Upsert
    suspend fun upsertRequest(request: PendingResidentRequestEntity)

    @Query(
        "UPDATE pending_resident_requests " +
            "SET deliveryState = :deliveryState, failureMessage = :message " +
            "WHERE clientRequestId = :clientRequestId",
    )
    suspend fun updateDeliveryState(
        clientRequestId: String,
        deliveryState: RequestDeliveryState,
        message: String?,
    )

    @Query("DELETE FROM pending_resident_requests WHERE clientRequestId = :clientRequestId")
    suspend fun deleteRequest(clientRequestId: String)
}
