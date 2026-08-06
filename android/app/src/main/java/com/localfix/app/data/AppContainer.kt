package com.localfix.app.data

import android.content.Context
import androidx.room.Room
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.draft.RoomRequestDraftRepository
import com.localfix.app.data.local.LocalFixDatabase
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.resident.SampleResidentRepository

interface AppContainer {
    val residentRepository: ResidentRepository
    val requestDraftRepository: RequestDraftRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database = Room.databaseBuilder(
        context,
        LocalFixDatabase::class.java,
        "localfix.db",
    ).build()

    override val residentRepository: ResidentRepository = SampleResidentRepository()
    override val requestDraftRepository: RequestDraftRepository =
        RoomRequestDraftRepository(database.requestDraftDao())
}
