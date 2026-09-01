package com.localfix.app.data.manager

import kotlinx.coroutines.flow.StateFlow

interface ManagerRepository {
    val managerData: StateFlow<ManagerData>
    val syncState: StateFlow<ManagerSyncState>

    suspend fun refresh()

    suspend fun createResidentInvite(
        unitId: String,
        validDays: Int = 7,
    ): ManagerResidentInvite

    suspend fun createWorkerInvite(
        name: String,
        specialty: com.localfix.app.data.model.ServiceCategory,
        validDays: Int = 7,
    ): ManagerWorkerInvite

    suspend fun assignTicket(
        ticketId: String,
        expectedVersion: Int,
        priority: ManagerPriority,
        workerId: String,
    ): ManagerTicket
}

sealed interface ManagerSyncState {
    data object InitialLoading : ManagerSyncState
    data object Refreshing : ManagerSyncState
    data object Ready : ManagerSyncState
    data class Error(
        val message: String,
        val hasPreviousResult: Boolean,
    ) : ManagerSyncState
}
