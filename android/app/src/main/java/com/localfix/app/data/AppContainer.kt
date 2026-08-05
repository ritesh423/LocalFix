package com.localfix.app.data

import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.resident.SampleResidentRepository

interface AppContainer {
    val residentRepository: ResidentRepository
}

class DefaultAppContainer : AppContainer {
    override val residentRepository: ResidentRepository = SampleResidentRepository()
}
