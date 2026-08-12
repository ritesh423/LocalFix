package com.localfix.app.data.resident

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
        val repository = ApiResidentRepository(ticketApi, clock)

        repository.refreshRequests()

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
        val repository = ApiResidentRepository(ticketApi, clock)

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
            clock = clock,
        )

        repository.refreshRequests()

        assertTrue(repository.requestSyncState.value is RequestSyncState.Error)
        assertEquals(emptyList<Any>(), repository.residentData.value.requests)
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

    private inner class FakeTicketApi(
        private val tickets: MutableList<TicketResponse> = mutableListOf(),
        private val listFailure: Throwable? = null,
    ) : TicketApi {
        var lastCreatePayload: TicketCreatePayload? = null

        override suspend fun createTicket(request: TicketCreatePayload): TicketResponse {
            lastCreatePayload = request
            return ticketResponse(title = request.title).also { tickets.add(0, it) }
        }

        override suspend fun listTickets(): List<TicketResponse> {
            listFailure?.let { throw it }
            return tickets
        }
    }
}
