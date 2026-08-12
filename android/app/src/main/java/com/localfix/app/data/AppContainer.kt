package com.localfix.app.data

import android.content.Context
import androidx.room.Room
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.draft.RoomRequestDraftRepository
import com.localfix.app.data.local.LocalFixDatabase
import com.localfix.app.data.local.MIGRATION_1_2
import com.localfix.app.data.local.MIGRATION_2_3
import com.localfix.app.data.remote.HttpTicketApi
import com.localfix.app.data.resident.ApiResidentRepository
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.R

interface AppContainer {
    val residentRepository: ResidentRepository
    val requestDraftRepository: RequestDraftRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database = Room.databaseBuilder(
        context,
        LocalFixDatabase::class.java,
        "localfix.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    override val residentRepository: ResidentRepository = ApiResidentRepository(
        ticketApi = HttpTicketApi(context.getString(R.string.api_base_url)),
    )
    override val requestDraftRepository: RequestDraftRepository =
        RoomRequestDraftRepository(database.requestDraftDao())
}
