package com.localfix.app.data.resident

import com.localfix.app.data.model.ResidentData
import kotlinx.coroutines.flow.StateFlow

interface ResidentRepository {
    val residentData: StateFlow<ResidentData>
}
