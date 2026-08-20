package com.localfix.app.data.resident

import com.localfix.app.data.local.PendingResidentRequestEntity
import com.localfix.app.data.local.PendingResidentReviewEntity
import com.localfix.app.data.local.ResidentRequestStore
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ResidentReviewDecision
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
import org.junit.Test
import java.io.IOException

class PendingReviewSyncerTest {
    @Test
    fun successfulReviewReplacesTheCachedTicketAndClearsTheQueue() = runTest {
        val store = FakeStore(pendingReview())
        val api = FakeApi(reviewedTicket())

        val result = PendingReviewSyncer(api, store).sync(TICKET_ID)

        assertEquals(PendingRequestSyncResult.SUCCESS, result)
        assertEquals(4, api.lastReviewPayload?.expectedVersion)
        assertEquals("confirm", api.lastReviewPayload?.decision)
        assertNull(store.getPendingReview(TICKET_ID))
        assertEquals(5, store.tickets.value.single().residentRating)
    }

    @Test
    fun networkFailureKeepsTheReviewPendingForWorkManager() = runTest {
        val store = FakeStore(pendingReview())

        val result = PendingReviewSyncer(
            FakeApi(reviewFailure = IOException("offline")),
            store,
        ).sync(TICKET_ID)

        assertEquals(PendingRequestSyncResult.RETRY, result)
        assertEquals(RequestDeliveryState.PENDING, store.getPendingReview(TICKET_ID)?.deliveryState)
    }

    @Test
    fun permanentServerRejectionKeepsTheReviewVisibleAsFailed() = runTest {
        val store = FakeStore(pendingReview())

        val result = PendingReviewSyncer(
            FakeApi(reviewFailure = TicketApiException(400, "invalid review")),
            store,
        ).sync(TICKET_ID)

        assertEquals(PendingRequestSyncResult.FAILURE, result)
        assertEquals(RequestDeliveryState.FAILED, store.getPendingReview(TICKET_ID)?.deliveryState)
    }

    @Test
    fun versionConflictIsAcceptedWhenTheServerAlreadyHasTheSameReview() = runTest {
        val store = FakeStore(pendingReview())

        val result = PendingReviewSyncer(
            FakeApi(
                reviewedTicket = reviewedTicket(),
                reviewFailure = TicketApiException(409, "version conflict"),
            ),
            store,
        ).sync(TICKET_ID)

        assertEquals(PendingRequestSyncResult.SUCCESS, result)
        assertNull(store.getPendingReview(TICKET_ID))
        assertEquals(5, store.tickets.value.single().residentRating)
    }

    private fun pendingReview() = PendingResidentReviewEntity(
        ticketId = TICKET_ID,
        expectedVersion = 4,
        decision = ResidentReviewDecision.CONFIRM,
        rating = 5,
        feedback = "The repair works properly now.",
        deliveryState = RequestDeliveryState.PENDING,
        failureMessage = null,
        queuedAt = "2026-08-18T10:00:00Z",
    )

    private fun reviewedTicket() = TicketResponse(
        id = TICKET_ID,
        clientRequestId = "50000000-0000-0000-0000-000000000004",
        unitId = "30000000-0000-0000-0000-000000000204",
        title = "Kitchen tap is leaking",
        description = "Water continues dripping after the tap is fully closed.",
        category = "plumbing",
        urgencySuggestion = "soon",
        accessWindow = "morning",
        status = "completed",
        version = 5,
        assignedWorker = "Arun Kumar",
        residentRating = 5,
        residentFeedback = "The repair works properly now.",
        residentReviewedAt = "2026-08-18T10:01:00Z",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-18T10:01:00Z",
    )

    private class FakeStore(
        review: PendingResidentReviewEntity,
    ) : ResidentRequestStore {
        val tickets = MutableStateFlow<List<ResidentTicketEntity>>(emptyList())
        private val reviews = MutableStateFlow(listOf(review))

        override fun observeTickets(): Flow<List<ResidentTicketEntity>> = tickets
        override fun observePendingRequests(): Flow<List<PendingResidentRequestEntity>> =
            MutableStateFlow(emptyList())
        override fun observePendingReviews(): Flow<List<PendingResidentReviewEntity>> = reviews
        override suspend fun hasLocalRequests(): Boolean = true
        override suspend fun getPendingRequest(clientRequestId: String) = null
        override suspend fun getRetryableRequestIds() = emptyList<String>()
        override suspend fun getPendingReview(ticketId: String) =
            reviews.value.find { it.ticketId == ticketId }
        override suspend fun getRetryableReviewIds() = reviews.value.map { it.ticketId }
        override suspend fun queueRequest(request: PendingResidentRequestEntity) = Unit
        override suspend fun markRequestFailed(clientRequestId: String, message: String) = Unit
        override suspend fun markRequestPending(clientRequestId: String) = Unit
        override suspend fun discardFailedRequest(clientRequestId: String) = Unit
        override suspend fun queueReview(review: PendingResidentReviewEntity) {
            reviews.value = listOf(review)
        }
        override suspend fun markReviewFailed(ticketId: String, message: String) {
            reviews.value = reviews.value.map {
                if (it.ticketId == ticketId) {
                    it.copy(deliveryState = RequestDeliveryState.FAILED, failureMessage = message)
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
        ) = Unit
        override suspend fun completePendingReview(
            ticketId: String,
            ticket: ResidentTicketEntity,
        ) {
            tickets.value = listOf(ticket)
            reviews.value = reviews.value.filterNot { it.ticketId == ticketId }
        }
        override suspend fun replaceServerSnapshot(
            tickets: List<ResidentTicketEntity>,
            acknowledgedClientRequestIds: List<String>,
        ) = Unit
    }

    private class FakeApi(
        private val reviewedTicket: TicketResponse? = null,
        private val reviewFailure: Throwable? = null,
    ) : TicketApi {
        var lastReviewPayload: TicketReviewPayload? = null

        override suspend fun createTicket(request: TicketCreatePayload): TicketResponse =
            error("Not used")

        override suspend fun listTickets(): List<TicketResponse> = listOfNotNull(reviewedTicket)

        override suspend fun reviewTicket(
            ticketId: String,
            request: TicketReviewPayload,
        ): TicketResponse {
            lastReviewPayload = request
            reviewFailure?.let { throw it }
            return requireNotNull(reviewedTicket)
        }
    }

    private companion object {
        const val TICKET_ID = "90000000-0000-0000-0000-000000000001"
    }
}
