package com.localfix.app.data.resident

import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.NewMaintenanceRequest
import kotlinx.coroutines.flow.StateFlow

interface ResidentRepository {
    val residentData: StateFlow<ResidentData>
    val requestSyncState: StateFlow<RequestSyncState>

    suspend fun createRequest(request: NewMaintenanceRequest): String

    suspend fun refreshRequests()

    suspend fun retryFailedRequest(clientRequestId: String)

    suspend fun discardFailedRequest(clientRequestId: String)

    suspend fun reviewRequest(
        ticketId: String,
        expectedVersion: Int,
        decision: ResidentReviewDecision,
        rating: Int?,
        feedback: String?,
    )
}

enum class ResidentReviewDecision {
    CONFIRM,
    REQUEST_REWORK,
}

sealed interface RequestSyncState {
    data object InitialLoading : RequestSyncState
    data object Refreshing : RequestSyncState
    data object Ready : RequestSyncState
    data class Error(
        val message: String,
        val hasPreviousResult: Boolean,
    ) : RequestSyncState
}
