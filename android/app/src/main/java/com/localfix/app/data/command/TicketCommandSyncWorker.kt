package com.localfix.app.data.command

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localfix.app.LocalFixApplication
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.resident.PendingRequestSyncResult

class TicketCommandSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(WorkManagerTicketCommandSyncScheduler.TICKET_ID_KEY)
            ?: return Result.failure()
        val commandType = inputData.getString(
            WorkManagerTicketCommandSyncScheduler.COMMAND_TYPE_KEY,
        )
            ?.let { runCatching { TicketCommandType.valueOf(it) }.getOrNull() }
            ?: return Result.failure()
        val application = applicationContext as LocalFixApplication
        return when (application.appContainer.ticketCommandSyncer.sync(ticketId, commandType)) {
            PendingRequestSyncResult.SUCCESS -> Result.success()
            PendingRequestSyncResult.RETRY -> Result.retry()
            PendingRequestSyncResult.FAILURE -> Result.failure()
        }
    }
}
