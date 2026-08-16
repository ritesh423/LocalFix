package com.localfix.app.data.manager

import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.remote.ManagerTicketApi
import com.localfix.app.data.remote.TicketAssignmentPayload
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.WorkerResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ApiManagerRepositoryTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-08-16T10:15:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun refreshMapsThePropertyQueueAndEligibleWorkers() = runTest {
        val repository = ApiManagerRepository(FakeManagerApi(), clock)

        repository.refresh()

        val data = repository.managerData.value
        assertEquals("Apartment A-204", data.tickets.single().unitLabel)
        assertEquals(TicketStatus.OPEN, data.tickets.single().status)
        assertEquals("Updated 15 min ago", data.tickets.single().updatedLabel)
        assertEquals(ServiceCategory.PLUMBING, data.workers.single().specialty)
        assertEquals(ManagerSyncState.Ready, repository.syncState.value)
    }

    @Test
    fun assignmentSendsVersionPriorityAndTrustedWorkerIdThenUpdatesQueue() = runTest {
        val api = FakeManagerApi()
        val repository = ApiManagerRepository(api, clock)
        repository.refresh()

        repository.assignTicket(
            ticketId = TICKET_ID,
            expectedVersion = 1,
            priority = ManagerPriority.SOON,
            workerId = WORKER_ID,
        )

        assertEquals(
            TicketAssignmentPayload(
                expectedVersion = 1,
                priority = "soon",
                workerId = WORKER_ID,
            ),
            api.lastAssignment,
        )
        val assigned = repository.managerData.value.tickets.single()
        assertEquals(TicketStatus.ASSIGNED, assigned.status)
        assertEquals("Arun Kumar", assigned.assignedWorker)
        assertEquals(2, assigned.version)
    }

    @Test
    fun failedFirstRefreshBecomesVisibleWithoutFakeQueueData() = runTest {
        val repository = ApiManagerRepository(
            ticketApi = FakeManagerApi(listFailure = IOException("offline")),
            clock = clock,
        )

        repository.refresh()

        val state = repository.syncState.value as ManagerSyncState.Error
        assertFalse(state.hasPreviousResult)
        assertTrue(repository.managerData.value.tickets.isEmpty())
    }

    private class FakeManagerApi(
        private val listFailure: Throwable? = null,
    ) : ManagerTicketApi {
        var lastAssignment: TicketAssignmentPayload? = null

        override suspend fun listManagerTickets(): List<TicketResponse> {
            listFailure?.let { throw it }
            return listOf(ticketResponse())
        }

        override suspend fun listManagerWorkers(): List<WorkerResponse> = listOf(
            WorkerResponse(
                id = WORKER_ID,
                name = "Arun Kumar",
                specialty = "plumbing",
            ),
        )

        override suspend fun assignTicket(
            ticketId: String,
            request: TicketAssignmentPayload,
        ): TicketResponse {
            assertEquals(TICKET_ID, ticketId)
            lastAssignment = request
            return ticketResponse().copy(
                priority = request.priority,
                status = "assigned",
                version = 2,
                assignedWorkerId = request.workerId,
                assignedWorker = "Arun Kumar",
                updatedAt = "2026-08-16T10:15:00Z",
            )
        }
    }

    private companion object {
        const val TICKET_ID = "90000000-0000-0000-0000-000000000001"
        const val WORKER_ID = "40000000-0000-0000-0000-000000000001"

        fun ticketResponse() = TicketResponse(
            id = TICKET_ID,
            clientRequestId = "50000000-0000-0000-0000-000000000001",
            propertyId = "10000000-0000-0000-0000-000000000001",
            unitId = "30000000-0000-0000-0000-000000000204",
            residentId = "20000000-0000-0000-0000-000000000001",
            title = "Bathroom pipe is leaking",
            description = "Water is collecting below the bathroom washbasin pipe.",
            category = "plumbing",
            urgencySuggestion = "soon",
            priority = null,
            accessWindow = "morning",
            status = "open",
            version = 1,
            assignedWorkerId = null,
            assignedWorker = null,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z",
        )
    }
}
