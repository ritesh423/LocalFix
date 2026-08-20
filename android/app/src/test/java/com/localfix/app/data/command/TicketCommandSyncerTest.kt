package com.localfix.app.data.command

import com.localfix.app.data.local.PendingTicketCommandEntity
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.remote.ManagerTicketApi
import com.localfix.app.data.remote.TicketApiException
import com.localfix.app.data.remote.TicketAssignmentPayload
import com.localfix.app.data.remote.TicketCompletionPayload
import com.localfix.app.data.remote.TicketEventResponse
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketStartPayload
import com.localfix.app.data.remote.WorkerResponse
import com.localfix.app.data.remote.WorkerTicketApi
import com.localfix.app.data.resident.PendingRequestSyncResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class TicketCommandSyncerTest {
    @Test
    fun assignmentUsesQueuedValuesAndClearsOnlyAfterSuccess() = runTest {
        val store = InMemoryTicketCommandStore(listOf(assignmentCommand()))
        val api = FakeCommandApi(assignedTicket())
        var accepted: TicketResponse? = null
        val syncer = TicketCommandSyncer(api, api, store, { accepted = it }, {})

        val result = syncer.sync(TICKET_ID, TicketCommandType.ASSIGN)

        assertEquals(PendingRequestSyncResult.SUCCESS, result)
        assertEquals("soon", api.lastAssignment?.priority)
        assertEquals(WORKER_ID, api.lastAssignment?.workerId)
        assertEquals("assigned", accepted?.status)
        assertNull(store.getCommand(TICKET_ID, TicketCommandType.ASSIGN))
    }

    @Test
    fun temporaryNetworkFailureLeavesTheCommandForWorkManagerRetry() = runTest {
        val store = InMemoryTicketCommandStore(listOf(startCommand()))
        val api = FakeCommandApi(startFailure = IOException("offline"))
        val syncer = TicketCommandSyncer(api, api, store, {}, {})

        val result = syncer.sync(TICKET_ID, TicketCommandType.START)

        assertEquals(PendingRequestSyncResult.RETRY, result)
        assertEquals(
            RequestDeliveryState.PENDING,
            store.getCommand(TICKET_ID, TicketCommandType.START)?.deliveryState,
        )
    }

    @Test
    fun startConflictSucceedsWhenTheServerAlreadyMovedPastAssigned() = runTest {
        val store = InMemoryTicketCommandStore(listOf(startCommand()))
        val api = FakeCommandApi(
            ticket = startedTicket(),
            startFailure = TicketApiException(409, "version conflict"),
        )
        var accepted: TicketResponse? = null
        val syncer = TicketCommandSyncer(api, api, store, {}, { accepted = it })

        val result = syncer.sync(TICKET_ID, TicketCommandType.START)

        assertEquals(PendingRequestSyncResult.SUCCESS, result)
        assertEquals("in_progress", accepted?.status)
        assertNull(store.getCommand(TICKET_ID, TicketCommandType.START))
    }

    private fun assignmentCommand() = PendingTicketCommandEntity(
        ticketId = TICKET_ID,
        commandType = TicketCommandType.ASSIGN,
        expectedVersion = 1,
        priority = "soon",
        workerId = WORKER_ID,
        deliveryState = RequestDeliveryState.PENDING,
        failureMessage = null,
        queuedAt = "2026-08-18T11:00:00Z",
    )

    private fun startCommand() = PendingTicketCommandEntity(
        ticketId = TICKET_ID,
        commandType = TicketCommandType.START,
        expectedVersion = 2,
        priority = null,
        workerId = null,
        deliveryState = RequestDeliveryState.PENDING,
        failureMessage = null,
        queuedAt = "2026-08-18T11:00:00Z",
    )

    private fun assignedTicket() = baseTicket().copy(
        status = "assigned",
        version = 2,
        priority = "soon",
        assignedWorkerId = WORKER_ID,
        assignedWorker = "Arun Kumar",
    )

    private fun startedTicket() = assignedTicket().copy(status = "in_progress", version = 3)

    private fun baseTicket() = TicketResponse(
        id = TICKET_ID,
        clientRequestId = "50000000-0000-0000-0000-000000000001",
        unitId = "30000000-0000-0000-0000-000000000204",
        title = "Bathroom pipe is leaking",
        description = "Water is collecting below the bathroom washbasin pipe.",
        category = "plumbing",
        urgencySuggestion = "soon",
        accessWindow = "morning",
        status = "open",
        version = 1,
        assignedWorker = null,
        createdAt = "2026-08-18T10:00:00Z",
        updatedAt = "2026-08-18T10:00:00Z",
    )

    private class FakeCommandApi(
        private val ticket: TicketResponse = baseResponse(),
        private val assignmentFailure: Throwable? = null,
        private val startFailure: Throwable? = null,
    ) : ManagerTicketApi, WorkerTicketApi {
        var lastAssignment: TicketAssignmentPayload? = null

        override suspend fun listManagerTickets() = listOf(ticket)
        override suspend fun listManagerWorkers() = emptyList<WorkerResponse>()
        override suspend fun assignTicket(
            ticketId: String,
            request: TicketAssignmentPayload,
        ): TicketResponse {
            lastAssignment = request
            assignmentFailure?.let { throw it }
            return ticket
        }
        override suspend fun listWorkerTickets() = listOf(ticket)
        override suspend fun listWorkerTicketEvents(ticketId: String) =
            emptyList<TicketEventResponse>()
        override suspend fun startTicket(
            ticketId: String,
            request: TicketStartPayload,
        ): TicketResponse {
            startFailure?.let { throw it }
            return ticket
        }
        override suspend fun submitCompletion(
            ticketId: String,
            request: TicketCompletionPayload,
        ): TicketResponse = error("Not used")

        companion object {
            private fun baseResponse() = TicketResponse(
                id = TICKET_ID,
                clientRequestId = "50000000-0000-0000-0000-000000000001",
                unitId = "30000000-0000-0000-0000-000000000204",
                title = "Bathroom pipe is leaking",
                description = "Water is collecting below the bathroom washbasin pipe.",
                category = "plumbing",
                urgencySuggestion = "soon",
                accessWindow = "morning",
                status = "open",
                version = 1,
                assignedWorker = null,
                createdAt = "2026-08-18T10:00:00Z",
                updatedAt = "2026-08-18T10:00:00Z",
            )
        }
    }

    private companion object {
        const val TICKET_ID = "90000000-0000-0000-0000-000000000001"
        const val WORKER_ID = "40000000-0000-0000-0000-000000000001"
    }
}
