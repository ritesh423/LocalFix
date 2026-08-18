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

fun interface PendingRequestSyncScheduler {
    fun schedule(clientRequestId: String, replaceExisting: Boolean)
}

class WorkManagerPendingRequestSyncScheduler(
    context: Context,
) : PendingRequestSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(clientRequestId: String, replaceExisting: Boolean) {
        val request = OneTimeWorkRequestBuilder<PendingRequestSyncWorker>()
            .setInputData(workDataOf(CLIENT_REQUEST_ID_KEY to clientRequestId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .build()
        workManager.enqueueUniqueWork(
            "$UNIQUE_WORK_PREFIX$clientRequestId",
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val CLIENT_REQUEST_ID_KEY = "client_request_id"
        private const val UNIQUE_WORK_PREFIX = "sync-resident-request-"
    }
}
