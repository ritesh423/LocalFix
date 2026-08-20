package com.localfix.app.data.resident

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

fun interface PendingReviewSyncScheduler {
    fun schedule(ticketId: String, replaceExisting: Boolean)
}

class WorkManagerPendingReviewSyncScheduler(
    context: Context,
) : PendingReviewSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(ticketId: String, replaceExisting: Boolean) {
        val request = OneTimeWorkRequestBuilder<PendingReviewSyncWorker>()
            .setInputData(workDataOf(TICKET_ID_KEY to ticketId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "$UNIQUE_WORK_PREFIX$ticketId",
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val TICKET_ID_KEY = "ticket_id"
        private const val UNIQUE_WORK_PREFIX = "sync-resident-review-"
    }
}
