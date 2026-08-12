package com.localfix.app.data.resident

import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.NewMaintenanceRequest
import kotlinx.coroutines.flow.StateFlow

interface ResidentRepository {
    val residentData: StateFlow<ResidentData>
    val requestSyncState: StateFlow<RequestSyncState>

    suspend fun createRequest(request: NewMaintenanceRequest): String

    suspend fun refreshRequests()
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
