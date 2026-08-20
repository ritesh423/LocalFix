package com.localfix.app.data.command

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.localfix.app.data.model.TicketCommandType
import java.util.concurrent.TimeUnit

fun interface TicketCommandSyncScheduler {
    fun schedule(ticketId: String, commandType: TicketCommandType, replaceExisting: Boolean)
}

class WorkManagerTicketCommandSyncScheduler(context: Context) : TicketCommandSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(
        ticketId: String,
        commandType: TicketCommandType,
        replaceExisting: Boolean,
    ) {
        val request = OneTimeWorkRequestBuilder<TicketCommandSyncWorker>()
            .setInputData(
                workDataOf(
                    TICKET_ID_KEY to ticketId,
                    COMMAND_TYPE_KEY to commandType.name,
                ),
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "$UNIQUE_WORK_PREFIX${commandType.name}-$ticketId",
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val TICKET_ID_KEY = "ticket_id"
        const val COMMAND_TYPE_KEY = "command_type"
        private const val UNIQUE_WORK_PREFIX = "sync-ticket-command-"
    }
}
