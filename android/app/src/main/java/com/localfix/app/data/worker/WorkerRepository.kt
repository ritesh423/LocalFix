package com.localfix.app.data.worker

import kotlinx.coroutines.flow.StateFlow

interface WorkerRepository {
    val workerData: StateFlow<WorkerData>
    val syncState: StateFlow<WorkerSyncState>

    suspend fun refresh()

    suspend fun loadJobHistory(ticketId: String): List<WorkerJobEvent>

    suspend fun startJob(
        ticketId: String,
        expectedVersion: Int,
    ): WorkerJob

    suspend fun submitCompletion(
        ticketId: String,
        expectedVersion: Int,
        completionNote: String,
        partsUsed: List<String>,
        photoUri: String,
    ): WorkerJob
}

sealed interface WorkerSyncState {
    data object InitialLoading : WorkerSyncState
    data object Refreshing : WorkerSyncState
    data object Ready : WorkerSyncState
    data class Error(
        val message: String,
        val hasPreviousResult: Boolean,
    ) : WorkerSyncState
}
