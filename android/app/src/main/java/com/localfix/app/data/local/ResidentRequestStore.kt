package com.localfix.app.data.local

import com.localfix.app.data.model.RequestDeliveryState
import kotlinx.coroutines.flow.Flow

interface ResidentRequestStore {
    fun observeTickets(): Flow<List<ResidentTicketEntity>>

    fun observePendingRequests(): Flow<List<PendingResidentRequestEntity>>

    suspend fun hasLocalRequests(): Boolean

    suspend fun getPendingRequest(clientRequestId: String): PendingResidentRequestEntity?

    suspend fun getRetryableRequestIds(): List<String>

    suspend fun queueRequest(request: PendingResidentRequestEntity)

    suspend fun markRequestFailed(clientRequestId: String, message: String)

    suspend fun markRequestPending(clientRequestId: String)

    suspend fun discardFailedRequest(clientRequestId: String)

    suspend fun upsertTicket(ticket: ResidentTicketEntity)

    suspend fun completePendingRequest(
        clientRequestId: String,
        ticket: ResidentTicketEntity,
    )

    suspend fun replaceServerSnapshot(
        tickets: List<ResidentTicketEntity>,
        acknowledgedClientRequestIds: List<String>,
    )
}

class RoomResidentRequestStore(
    private val ticketDao: ResidentTicketDao,
    private val pendingRequestDao: PendingResidentRequestDao,
    private val syncDao: ResidentRequestSyncDao,
) : ResidentRequestStore {
    override fun observeTickets(): Flow<List<ResidentTicketEntity>> =
        ticketDao.observeTickets()

    override fun observePendingRequests(): Flow<List<PendingResidentRequestEntity>> =
        pendingRequestDao.observeRequests()

    override suspend fun hasLocalRequests(): Boolean =
        ticketDao.countTickets() > 0 || pendingRequestDao.countRequests() > 0

    override suspend fun getPendingRequest(
        clientRequestId: String,
    ): PendingResidentRequestEntity? = pendingRequestDao.getRequest(clientRequestId)

    override suspend fun getRetryableRequestIds(): List<String> =
        pendingRequestDao.getRetryableRequestIds()

    override suspend fun queueRequest(request: PendingResidentRequestEntity) {
        pendingRequestDao.upsertRequest(request)
    }

    override suspend fun markRequestFailed(clientRequestId: String, message: String) {
        pendingRequestDao.updateDeliveryState(
            clientRequestId = clientRequestId,
            deliveryState = RequestDeliveryState.FAILED,
            message = message,
        )
    }

    override suspend fun markRequestPending(clientRequestId: String) {
        pendingRequestDao.updateDeliveryState(
            clientRequestId = clientRequestId,
            deliveryState = RequestDeliveryState.PENDING,
            message = null,
        )
    }

    override suspend fun discardFailedRequest(clientRequestId: String) {
        pendingRequestDao.deleteRequest(clientRequestId)
    }

    override suspend fun upsertTicket(ticket: ResidentTicketEntity) {
        ticketDao.upsertTicket(ticket)
    }

    override suspend fun completePendingRequest(
        clientRequestId: String,
        ticket: ResidentTicketEntity,
    ) {
        syncDao.completePendingRequest(clientRequestId, ticket)
    }

    override suspend fun replaceServerSnapshot(
        tickets: List<ResidentTicketEntity>,
        acknowledgedClientRequestIds: List<String>,
    ) {
        syncDao.replaceServerSnapshot(tickets, acknowledgedClientRequestIds)
    }
}
