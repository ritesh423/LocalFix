package com.localfix.app.data.resident

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localfix.app.LocalFixApplication

class PendingRequestSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val clientRequestId = inputData.getString(
            WorkManagerPendingRequestSyncScheduler.CLIENT_REQUEST_ID_KEY,
        ) ?: return Result.failure()
        val application = applicationContext as LocalFixApplication
        return when (application.appContainer.pendingRequestSyncer.sync(clientRequestId)) {
            PendingRequestSyncResult.SUCCESS -> Result.success()
            PendingRequestSyncResult.RETRY -> Result.retry()
            PendingRequestSyncResult.FAILURE -> Result.failure()
        }
    }
}
