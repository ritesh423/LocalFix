package com.localfix.app.data.resident

import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.NewMaintenanceRequest
import kotlinx.coroutines.flow.StateFlow

interface ResidentRepository {
    val residentData: StateFlow<ResidentData>

    suspend fun createRequest(request: NewMaintenanceRequest): String
}
