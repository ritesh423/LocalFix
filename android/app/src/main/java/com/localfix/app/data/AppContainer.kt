package com.localfix.app.data

import android.content.Context
import androidx.room.Room
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.draft.RoomRequestDraftRepository
import com.localfix.app.data.local.LocalFixDatabase
import com.localfix.app.data.local.MIGRATION_1_2
import com.localfix.app.data.local.MIGRATION_2_3
import com.localfix.app.data.manager.ApiManagerRepository
import com.localfix.app.data.manager.ManagerRepository
import com.localfix.app.data.remote.HttpTicketApi
import com.localfix.app.data.resident.ApiResidentRepository
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.worker.ApiWorkerRepository
import com.localfix.app.data.worker.WorkerRepository
import com.localfix.app.R

interface AppContainer {
    val residentRepository: ResidentRepository
    val managerRepository: ManagerRepository
    val workerRepository: WorkerRepository
    val requestDraftRepository: RequestDraftRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database = Room.databaseBuilder(
        context,
        LocalFixDatabase::class.java,
        "localfix.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    private val ticketApi = HttpTicketApi(
        baseUrl = context.getString(R.string.api_base_url),
        contentResolver = context.contentResolver,
    )

    override val residentRepository: ResidentRepository = ApiResidentRepository(
        ticketApi = ticketApi,
    )
    override val managerRepository: ManagerRepository = ApiManagerRepository(ticketApi)
    override val workerRepository: WorkerRepository = ApiWorkerRepository(ticketApi)
    override val requestDraftRepository: RequestDraftRepository =
        RoomRequestDraftRepository(database.requestDraftDao())
}
