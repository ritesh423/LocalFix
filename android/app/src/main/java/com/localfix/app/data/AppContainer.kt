package com.localfix.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import com.localfix.app.R
import com.localfix.app.data.command.RoomTicketCommandStore
import com.localfix.app.data.command.TicketCommandSyncer
import com.localfix.app.data.command.WorkManagerTicketCommandSyncScheduler
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.draft.RoomRequestDraftRepository
import com.localfix.app.data.local.LocalFixDatabase
import com.localfix.app.data.local.MIGRATION_1_2
import com.localfix.app.data.local.MIGRATION_2_3
import com.localfix.app.data.local.MIGRATION_3_4
import com.localfix.app.data.local.MIGRATION_4_5
import com.localfix.app.data.local.MIGRATION_5_6
import com.localfix.app.data.local.MIGRATION_6_7
import com.localfix.app.data.local.MIGRATION_7_8
import com.localfix.app.data.local.RoomResidentRequestStore
import com.localfix.app.data.manager.ApiManagerRepository
import com.localfix.app.data.manager.ManagerRepository
import com.localfix.app.data.remote.HttpTicketApi
import com.localfix.app.data.resident.ApiResidentRepository
import com.localfix.app.data.resident.PendingRequestSyncer
import com.localfix.app.data.resident.PendingReviewSyncer
import com.localfix.app.data.resident.WorkManagerPendingReviewSyncScheduler
import com.localfix.app.data.resident.WorkManagerPendingRequestSyncScheduler
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.worker.ApiWorkerRepository
import com.localfix.app.data.worker.WorkerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val residentRepository: ResidentRepository
    val managerRepository: ManagerRepository
    val workerRepository: WorkerRepository
    val requestDraftRepository: RequestDraftRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = Room.databaseBuilder(
        context,
        LocalFixDatabase::class.java,
        "localfix.db",
    ).addMigrations(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    )
        .build()

    private val ticketApi = HttpTicketApi(
        baseUrl = context.getString(R.string.api_base_url),
        contentResolver = context.contentResolver,
    )
    private val residentRequestStore = RoomResidentRequestStore(
        ticketDao = database.residentTicketDao(),
        pendingRequestDao = database.pendingResidentRequestDao(),
        pendingReviewDao = database.pendingResidentReviewDao(),
        syncDao = database.residentRequestSyncDao(),
    )
    private val pendingRequestSyncScheduler = WorkManagerPendingRequestSyncScheduler(context)
    private val pendingReviewSyncScheduler = WorkManagerPendingReviewSyncScheduler(context)
    private val ticketCommandStore = RoomTicketCommandStore(database.pendingTicketCommandDao())
    private val ticketCommandSyncScheduler = WorkManagerTicketCommandSyncScheduler(context)

    val pendingRequestSyncer = PendingRequestSyncer(
        ticketApi = ticketApi,
        residentRequestStore = residentRequestStore,
    )
    val pendingReviewSyncer = PendingReviewSyncer(
        ticketApi = ticketApi,
        residentRequestStore = residentRequestStore,
    )

    override val residentRepository: ResidentRepository = ApiResidentRepository(
        ticketApi = ticketApi,
        residentRequestStore = residentRequestStore,
        pendingRequestSyncScheduler = pendingRequestSyncScheduler,
        pendingReviewSyncScheduler = pendingReviewSyncScheduler,
        applicationScope = applicationScope,
    )
    private val apiManagerRepository = ApiManagerRepository(
        ticketApi = ticketApi,
        commandStore = ticketCommandStore,
        commandSyncScheduler = ticketCommandSyncScheduler,
        applicationScope = applicationScope,
    )
    private val apiWorkerRepository = ApiWorkerRepository(
        ticketApi = ticketApi,
        commandStore = ticketCommandStore,
        commandSyncScheduler = ticketCommandSyncScheduler,
        applicationScope = applicationScope,
    )
    val ticketCommandSyncer = TicketCommandSyncer(
        managerApi = ticketApi,
        workerApi = ticketApi,
        store = ticketCommandStore,
        onAssignmentSynced = apiManagerRepository::acceptSyncedTicket,
        onStartSynced = apiWorkerRepository::acceptSyncedTicket,
        onCompletionSynced = apiWorkerRepository::acceptSyncedTicket,
        onCompletionPhotoReleased = { photoUri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(photoUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        },
    )

    override val managerRepository: ManagerRepository = apiManagerRepository
    override val workerRepository: WorkerRepository = apiWorkerRepository
    override val requestDraftRepository: RequestDraftRepository =
        RoomRequestDraftRepository(database.requestDraftDao())

    init {
        applicationScope.launch {
            residentRequestStore.getRetryableRequestIds().forEach { clientRequestId ->
                pendingRequestSyncScheduler.schedule(
                    clientRequestId,
                    replaceExisting = false,
                )
            }
            residentRequestStore.getRetryableReviewIds().forEach { ticketId ->
                pendingReviewSyncScheduler.schedule(ticketId, replaceExisting = false)
            }
            ticketCommandStore.getRetryableCommands().forEach { command ->
                ticketCommandSyncScheduler.schedule(
                    command.ticketId,
                    command.commandType,
                    replaceExisting = false,
                )
            }
        }
    }
}
