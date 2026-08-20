package com.localfix.app.data.resident

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localfix.app.LocalFixApplication

class PendingReviewSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(
            WorkManagerPendingReviewSyncScheduler.TICKET_ID_KEY,
        ) ?: return Result.failure()
        val application = applicationContext as LocalFixApplication
        return when (application.appContainer.pendingReviewSyncer.sync(ticketId)) {
            PendingRequestSyncResult.SUCCESS -> Result.success()
            PendingRequestSyncResult.RETRY -> Result.retry()
            PendingRequestSyncResult.FAILURE -> Result.failure()
        }
    }
}
