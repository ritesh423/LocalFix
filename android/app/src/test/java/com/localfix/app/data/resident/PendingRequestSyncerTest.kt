package com.localfix.app.data.resident

import com.localfix.app.data.local.PendingResidentRequestEntity
import com.localfix.app.data.local.ResidentRequestStore
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketApiException
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketReviewPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PendingRequestSyncerTest {
    @Test
    fun successfulSyncUsesStableIdAndAtomicallyReplacesPendingRequest() = runTest {
        val store = FakeResidentRequestStore(pendingRequest())
        val api = FakeTicketApi()
        val syncer = PendingRequestSyncer(api, store)

        val result = syncer.sync(CLIENT_REQUEST_ID)

        assertEquals(PendingRequestSyncResult.SUCCESS, result)
        assertEquals(CLIENT_REQUEST_ID, api.lastCreatePayload?.clientRequestId)
        assertNull(store.getPendingRequest(CLIENT_REQUEST_ID))
        assertEquals("Kitchen tap is leaking", store.tickets.value.single().title)
    }

    @Test
    fun networkFailureKeepsRequestPendingForWorkManagerRetry() = runTest {
        val store = FakeResidentRequestStore(pendingRequest())
        val syncer = PendingRequestSyncer(
            FakeTicketApi(createFailure = IOException("offline")),
            store,
        )

        val result = syncer.sync(CLIENT_REQUEST_ID)

        assertEquals(PendingRequestSyncResult.RETRY, result)
        assertEquals(
            RequestDeliveryState.PENDING,
            store.getPendingRequest(CLIENT_REQUEST_ID)?.deliveryState,
        )
        assertTrue(store.tickets.value.isEmpty())
    }

    @Test
    fun invalidServerRequestBecomesAVisiblePermanentFailure() = runTest {
        val store = FakeResidentRequestStore(pendingRequest())
        val syncer = PendingRequestSyncer(
            FakeTicketApi(createFailure = TicketApiException(422, "invalid request")),
            store,
        )

        val result = syncer.sync(CLIENT_REQUEST_ID)

        assertEquals(PendingRequestSyncResult.FAILURE, result)
        val failed = store.getPendingRequest(CLIENT_REQUEST_ID)
        assertEquals(RequestDeliveryState.FAILED, failed?.deliveryState)
        assertTrue(failed?.failureMessage?.isNotBlank() == true)
    }

    private fun pendingRequest() = PendingResidentRequestEntity(
        clientRequestId = CLIENT_REQUEST_ID,
        title = "Kitchen tap is leaking",
        description = "Water continues dripping after the tap is fully closed.",
        category = ServiceCategory.PLUMBING,
        urgencySuggestion = UrgencySuggestion.SOON,
        accessWindow = AccessWindow.MORNING,
        photoUri = "content://localfix/photo/kitchen-tap",
        deliveryState = RequestDeliveryState.PENDING,
        failureMessage = null,
        queuedAt = "2026-08-17T10:00:00Z",
    )

    private class FakeResidentRequestStore(
        pendingRequest: PendingResidentRequestEntity,
    ) : ResidentRequestStore {
        val tickets = MutableStateFlow<List<ResidentTicketEntity>>(emptyList())
        private val pending = MutableStateFlow(listOf(pendingRequest))

        override fun observeTickets(): Flow<List<ResidentTicketEntity>> = tickets

        override fun observePendingRequests(): Flow<List<PendingResidentRequestEntity>> = pending

        override suspend fun hasLocalRequests(): Boolean = pending.value.isNotEmpty()

        override suspend fun getPendingRequest(
            clientRequestId: String,
        ): PendingResidentRequestEntity? = pending.value.find {
            it.clientRequestId == clientRequestId
        }

        override suspend fun getRetryableRequestIds(): List<String> = pending.value.map {
            it.clientRequestId
        }

        override suspend fun queueRequest(request: PendingResidentRequestEntity) {
            pending.value = listOf(request)
        }

        override suspend fun markRequestFailed(clientRequestId: String, message: String) {
            pending.value = pending.value.map {
                if (it.clientRequestId == clientRequestId) {
                    it.copy(
                        deliveryState = RequestDeliveryState.FAILED,
                        failureMessage = message,
                    )
                } else {
                    it
                }
            }
        }

        override suspend fun upsertTicket(ticket: ResidentTicketEntity) {
            tickets.value = listOf(ticket)
        }

        override suspend fun completePendingRequest(
            clientRequestId: String,
            ticket: ResidentTicketEntity,
        ) {
            tickets.value = listOf(ticket)
            pending.value = pending.value.filterNot { it.clientRequestId == clientRequestId }
        }

        override suspend fun replaceServerSnapshot(
            tickets: List<ResidentTicketEntity>,
            acknowledgedClientRequestIds: List<String>,
        ) {
            this.tickets.value = tickets
        }
    }

    private class FakeTicketApi(
        private val createFailure: Throwable? = null,
    ) : TicketApi {
        var lastCreatePayload: TicketCreatePayload? = null

        override suspend fun createTicket(request: TicketCreatePayload): TicketResponse {
            createFailure?.let { throw it }
            lastCreatePayload = request
            return ticketResponse(request)
        }

        override suspend fun listTickets(): List<TicketResponse> = emptyList()

        override suspend fun reviewTicket(
            ticketId: String,
            request: TicketReviewPayload,
        ): TicketResponse = error("Not used")

        private fun ticketResponse(request: TicketCreatePayload) = TicketResponse(
            id = "90000000-0000-0000-0000-000000000001",
            clientRequestId = request.clientRequestId,
            unitId = "30000000-0000-0000-0000-000000000204",
            title = request.title,
            description = request.description,
            category = request.category,
            urgencySuggestion = request.urgencySuggestion,
            accessWindow = request.accessWindow,
            status = "open",
            version = 1,
            assignedWorker = null,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z",
        )
    }

    private companion object {
        const val CLIENT_REQUEST_ID = "50000000-0000-0000-0000-000000000004"
    }
}
