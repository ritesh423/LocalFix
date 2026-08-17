package com.localfix.app.data.resident

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.local.ResidentTicketDao
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketReviewPayload
import com.localfix.app.data.remote.TicketResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ApiResidentRepositoryTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-08-12T10:05:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun refreshMapsBackendTicketsIntoAndroidModels() = runTest {
        val ticketApi = FakeTicketApi(
            tickets = mutableListOf(ticketResponse()),
        )
        val repository = ApiResidentRepository(ticketApi, FakeResidentTicketDao(), backgroundScope, clock)

        repository.refreshRequests()
        runCurrent()

        val request = repository.residentData.value.requests.single()
        assertEquals("90000000-0000-0000-0000-000000000001", request.id)
        assertEquals(ServiceCategory.PLUMBING, request.category)
        assertEquals(TicketStatus.OPEN, request.status)
        assertEquals("Awaiting assignment", request.assignedWorker)
        assertEquals("Updated 5 min ago", request.updatedLabel)
        assertEquals(RequestSyncState.Ready, repository.requestSyncState.value)
    }

    @Test
    fun createSendsStableClientRequestIdAndUpdatesLocalState() = runTest {
        val ticketApi = FakeTicketApi()
        val repository = ApiResidentRepository(ticketApi, FakeResidentTicketDao(), backgroundScope, clock)

        val requestId = repository.createRequest(
            NewMaintenanceRequest(
                clientRequestId = "50000000-0000-0000-0000-000000000004",
                title = "Kitchen tap is leaking",
                description = "Water continues dripping after the tap is fully closed.",
                category = ServiceCategory.PLUMBING,
                urgencySuggestion = UrgencySuggestion.SOON,
                accessWindow = AccessWindow.MORNING,
            ),
        )
        runCurrent()

        assertEquals("90000000-0000-0000-0000-000000000001", requestId)
        assertEquals(
            "50000000-0000-0000-0000-000000000004",
            ticketApi.lastCreatePayload?.clientRequestId,
        )
        assertEquals("Kitchen tap is leaking", repository.residentData.value.requests.single().title)
    }

    @Test
    fun connectionFailureBecomesVisibleRepositoryState() = runTest {
        val repository = ApiResidentRepository(
            ticketApi = FakeTicketApi(listFailure = IOException("offline")),
            residentTicketDao = FakeResidentTicketDao(),
            applicationScope = backgroundScope,
            clock = clock,
        )

        repository.refreshRequests()

        val state = repository.requestSyncState.value as RequestSyncState.Error
        assertEquals(false, state.hasPreviousResult)
        assertEquals(emptyList<Any>(), repository.residentData.value.requests)
    }

    @Test
    fun failedRefreshKeepsTheLastSuccessfulResult() = runTest {
        val ticketApi = FakeTicketApi(
            tickets = mutableListOf(ticketResponse()),
        )
        val repository = ApiResidentRepository(ticketApi, FakeResidentTicketDao(), backgroundScope, clock)
        repository.refreshRequests()
        runCurrent()
        ticketApi.listFailure = IOException("offline")

        repository.refreshRequests()

        val state = repository.requestSyncState.value as RequestSyncState.Error
        assertTrue(state.hasPreviousResult)
        assertEquals("Leaking kitchen tap", repository.residentData.value.requests.single().title)
    }

    @Test
    fun awaitingConfirmationMapsWorkerEvidenceAndProtectedPhotoUrl() = runTest {
        val ticketApi = FakeTicketApi(
            tickets = mutableListOf(
                ticketResponse().copy(
                    status = "awaiting_confirmation",
                    version = 4,
                    completionNote = "Replaced the damaged switch and tested it safely.",
                    partsUsed = listOf("16A modular switch"),
                    hasCompletionPhoto = true,
                ),
            ),
        )
        val repository = ApiResidentRepository(ticketApi, FakeResidentTicketDao(), backgroundScope, clock)

        repository.refreshRequests()
        runCurrent()

        val request = repository.residentData.value.requests.single()
        assertEquals(TicketStatus.AWAITING_CONFIRMATION, request.status)
        assertEquals("Replaced the damaged switch and tested it safely.", request.completionNote)
        assertEquals(listOf("16A modular switch"), request.partsUsed)
        assertEquals(
            "https://api.localfix.test/tickets/${request.id}/completion-photo",
            request.completionPhotoUrl,
        )
    }

    @Test
    fun reviewSendsCurrentVersionAndReplacesTheLocalRequest() = runTest {
        val ticketApi = FakeTicketApi(
            tickets = mutableListOf(
                ticketResponse().copy(status = "awaiting_confirmation", version = 4),
            ),
        )
        val repository = ApiResidentRepository(ticketApi, FakeResidentTicketDao(), backgroundScope, clock)
        repository.refreshRequests()
        runCurrent()

        repository.reviewRequest(
            ticketId = "90000000-0000-0000-0000-000000000001",
            expectedVersion = 4,
            decision = ResidentReviewDecision.CONFIRM,
            rating = 5,
            feedback = "Repair looks good.",
        )
        runCurrent()

        assertEquals(4, ticketApi.lastReviewPayload?.expectedVersion)
        assertEquals("confirm", ticketApi.lastReviewPayload?.decision)
        assertEquals(5, ticketApi.lastReviewPayload?.rating)
        val reviewed = repository.residentData.value.requests.single()
        assertEquals(TicketStatus.COMPLETED, reviewed.status)
        assertEquals(5, reviewed.residentRating)
    }

    @Test
    fun cachedTicketsAreShownWhenARefreshFailsAfterRepositoryRecreation() = runTest {
        val cachedTicket = ticketEntity(title = "Cached plumbing request")
        val ticketDao = FakeResidentTicketDao(listOf(cachedTicket))
        val repository = ApiResidentRepository(
            ticketApi = FakeTicketApi(listFailure = IOException("offline")),
            residentTicketDao = ticketDao,
            applicationScope = backgroundScope,
            clock = clock,
        )
        runCurrent()

        repository.refreshRequests()
        runCurrent()

        assertEquals("Cached plumbing request", repository.residentData.value.requests.single().title)
        val state = repository.requestSyncState.value as RequestSyncState.Error
        assertTrue(state.hasPreviousResult)
    }

    private fun ticketResponse(
        title: String = "Leaking kitchen tap",
    ) = TicketResponse(
        id = "90000000-0000-0000-0000-000000000001",
        clientRequestId = "50000000-0000-0000-0000-000000000004",
        unitId = "30000000-0000-0000-0000-000000000204",
        title = title,
        description = "The tap keeps dripping even when fully closed.",
        category = "plumbing",
        urgencySuggestion = "soon",
        accessWindow = "morning",
        status = "open",
        version = 1,
        assignedWorker = null,
        createdAt = "2026-08-12T10:00:00Z",
        updatedAt = "2026-08-12T10:00:00Z",
    )

    private fun ticketEntity(
        title: String,
    ) = ResidentTicketEntity(
        id = "90000000-0000-0000-0000-000000000001",
        propertyId = "10000000-0000-0000-0000-000000000001",
        unitId = "30000000-0000-0000-0000-000000000204",
        residentId = "40000000-0000-0000-0000-000000000001",
        title = title,
        description = "The tap keeps dripping even when fully closed.",
        category = ServiceCategory.PLUMBING,
        status = TicketStatus.OPEN,
        urgencySuggestion = UrgencySuggestion.SOON,
        accessWindow = AccessWindow.MORNING,
        assignedWorker = null,
        version = 1,
        completionNote = null,
        partsUsed = emptyList(),
        hasCompletionPhoto = false,
        residentRating = null,
        residentFeedback = null,
        createdAt = "2026-08-12T10:00:00Z",
        updatedAt = "2026-08-12T10:00:00Z",
    )

    private class FakeResidentTicketDao(
        initialTickets: List<ResidentTicketEntity> = emptyList(),
    ) : ResidentTicketDao {
        private val tickets = MutableStateFlow(initialTickets.sortedByDescending { it.updatedAt })

        override fun observeTickets(): Flow<List<ResidentTicketEntity>> = tickets

        override suspend fun countTickets(): Int = tickets.value.size

        override suspend fun upsertTicket(ticket: ResidentTicketEntity) {
            upsertTickets(listOf(ticket))
        }

        override suspend fun upsertTickets(tickets: List<ResidentTicketEntity>) {
            val incomingById = tickets.associateBy(ResidentTicketEntity::id)
            this.tickets.value = (this.tickets.value.filterNot { it.id in incomingById } + tickets)
                .sortedByDescending { it.updatedAt }
        }

        override suspend fun deleteAllTickets() {
            tickets.value = emptyList()
        }
    }

    private inner class FakeTicketApi(
        private val tickets: MutableList<TicketResponse> = mutableListOf(),
        var listFailure: Throwable? = null,
    ) : TicketApi {
        var lastCreatePayload: TicketCreatePayload? = null
        var lastReviewPayload: TicketReviewPayload? = null

        override suspend fun createTicket(request: TicketCreatePayload): TicketResponse {
            lastCreatePayload = request
            return ticketResponse(title = request.title).also { tickets.add(0, it) }
        }

        override suspend fun listTickets(): List<TicketResponse> {
            listFailure?.let { throw it }
            return tickets
        }

        override suspend fun reviewTicket(
            ticketId: String,
            request: TicketReviewPayload,
        ): TicketResponse {
            lastReviewPayload = request
            val current = tickets.single { it.id == ticketId }
            val reviewed = current.copy(
                status = if (request.decision == "confirm") "completed" else "assigned",
                version = current.version + 1,
                residentRating = request.rating,
                residentFeedback = request.feedback,
                residentReviewedAt = "2026-08-12T10:05:00Z",
                updatedAt = "2026-08-12T10:05:00Z",
            )
            tickets.replaceAll { if (it.id == ticketId) reviewed else it }
            return reviewed
        }

        override fun completionPhotoUrl(ticketId: String): String =
            "https://api.localfix.test/tickets/$ticketId/completion-photo"
    }
}
