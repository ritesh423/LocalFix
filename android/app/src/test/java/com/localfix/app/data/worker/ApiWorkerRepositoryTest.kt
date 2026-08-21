package com.localfix.app.data.worker

import com.localfix.app.data.command.InMemoryTicketCommandStore
import com.localfix.app.data.command.TicketCommandSyncScheduler
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketEventResponse
import com.localfix.app.data.remote.TicketCompletionPayload
import com.localfix.app.data.remote.TicketStartPayload
import com.localfix.app.data.remote.WorkerTicketApi
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiWorkerRepositoryTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-08-16T10:15:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun refreshMapsAssignedBackendTicketsIntoWorkerJobs() = runTest {
        val repository = repository(FakeWorkerApi(), backgroundScope)

        repository.refresh()
        runCurrent()

        val job = repository.workerData.value.jobs.single()
        assertEquals("Apartment A-204", job.unitLabel)
        assertEquals(ServiceCategory.PLUMBING, job.category)
        assertEquals(TicketStatus.ASSIGNED, job.status)
        assertEquals("Soon", job.priorityLabel)
        assertEquals("Updated 15 min ago", job.updatedLabel)
        assertEquals(WorkerSyncState.Ready, repository.syncState.value)
    }

    @Test
    fun startingAJobIsSavedAndScheduledBeforeAnyNetworkCall() = runTest {
        val api = FakeWorkerApi()
        val store = InMemoryTicketCommandStore()
        val scheduler = FakeCommandScheduler()
        val repository = ApiWorkerRepository(
            api,
            store,
            scheduler,
            backgroundScope,
            clock,
        )
        repository.refresh()
        runCurrent()

        repository.startJob(TICKET_ID, expectedVersion = 2)
        runCurrent()

        assertEquals(null, api.lastStartPayload)
        assertEquals(2, store.getCommand(TICKET_ID, TicketCommandType.START)?.expectedVersion)
        assertEquals(listOf(TICKET_ID), scheduler.ticketIds)
        val job = repository.workerData.value.jobs.single()
        assertEquals(TicketStatus.ASSIGNED, job.status)
        assertEquals(RequestDeliveryState.PENDING, job.startDeliveryState)
    }

    @Test
    fun completionIsSavedAndScheduledBeforeAnyPhotoUpload() = runTest {
        val api = FakeWorkerApi()
        val store = InMemoryTicketCommandStore()
        val scheduler = FakeCommandScheduler()
        val repository = ApiWorkerRepository(
            api,
            store,
            scheduler,
            backgroundScope,
            clock,
        )
        repository.refresh()
        runCurrent()

        repository.submitCompletion(
            ticketId = TICKET_ID,
            expectedVersion = 2,
            completionNote = "Replaced the washer and tested the tap.",
            partsUsed = listOf("Rubber washer"),
            photoUri = "content://localfix/completion-photo",
        )
        runCurrent()

        assertEquals(null, api.lastCompletionPayload)
        val command = store.getCommand(TICKET_ID, TicketCommandType.COMPLETE)
        assertEquals(2, command?.expectedVersion)
        assertEquals("Replaced the washer and tested the tap.", command?.completionNote)
        assertEquals(listOf("Rubber washer"), command?.partsUsed)
        assertEquals("content://localfix/completion-photo", command?.photoUri)
        assertEquals(listOf(TicketCommandType.COMPLETE), scheduler.commandTypes)
        val job = repository.workerData.value.jobs.single()
        assertEquals(RequestDeliveryState.PENDING, job.completionDeliveryState)
        assertEquals("Replaced the washer and tested the tap.", job.pendingCompletionNote)
    }

    @Test
    fun failedFirstRefreshShowsAnErrorWithoutInventingJobs() = runTest {
        val repository = ApiWorkerRepository(
            ticketApi = FakeWorkerApi(listFailure = IOException("offline")),
            commandStore = InMemoryTicketCommandStore(),
            commandSyncScheduler = FakeCommandScheduler(),
            applicationScope = backgroundScope,
            clock = clock,
        )

        repository.refresh()

        val state = repository.syncState.value as WorkerSyncState.Error
        assertFalse(state.hasPreviousResult)
        assertTrue(repository.workerData.value.jobs.isEmpty())
    }

    @Test
    fun reworkReasonAndImmutableHistoryAreMappedForTheWorker() = runTest {
        val repository = repository(FakeWorkerApi(), backgroundScope)
        repository.refresh()
        runCurrent()

        val history = repository.loadJobHistory(TICKET_ID)

        assertEquals(
            "The lower pipe joint is still dripping.",
            repository.workerData.value.jobs.single().reworkReason,
        )
        assertEquals("Resident requested more work", history.single().title)
        assertEquals("Ready to start", history.single().statusLabel)
        assertEquals(5, history.single().ticketVersion)
    }

    private fun repository(
        api: WorkerTicketApi,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = ApiWorkerRepository(
        ticketApi = api,
        commandStore = InMemoryTicketCommandStore(),
        commandSyncScheduler = FakeCommandScheduler(),
        applicationScope = scope,
        clock = clock,
    )

    private class FakeCommandScheduler : TicketCommandSyncScheduler {
        val ticketIds = mutableListOf<String>()
        val commandTypes = mutableListOf<TicketCommandType>()

        override fun schedule(
            ticketId: String,
            commandType: TicketCommandType,
            replaceExisting: Boolean,
        ) {
            ticketIds += ticketId
            commandTypes += commandType
        }
    }

    private class FakeWorkerApi(
        private val listFailure: Throwable? = null,
    ) : WorkerTicketApi {
        var lastStartPayload: TicketStartPayload? = null
        var lastCompletionPayload: TicketCompletionPayload? = null

        override suspend fun listWorkerTickets(): List<TicketResponse> {
            listFailure?.let { throw it }
            return listOf(ticketResponse())
        }

        override suspend fun listWorkerTicketEvents(
            ticketId: String,
        ): List<TicketEventResponse> = listOf(
            TicketEventResponse(
                id = "70000000-0000-0000-0000-000000000001",
                ticketId = ticketId,
                actorRole = "resident",
                action = "request_rework",
                fromStatus = "awaiting_confirmation",
                toStatus = "assigned",
                ticketVersion = 5,
                detail = "The lower pipe joint is still dripping.",
                createdAt = "2026-08-16T10:15:00Z",
            ),
        )

        override suspend fun startTicket(
            ticketId: String,
            request: TicketStartPayload,
        ): TicketResponse {
            assertEquals(TICKET_ID, ticketId)
            lastStartPayload = request
            return ticketResponse().copy(
                status = "in_progress",
                version = 3,
                updatedAt = "2026-08-16T10:15:00Z",
            )
        }

        override suspend fun submitCompletion(
            ticketId: String,
            request: TicketCompletionPayload,
        ): TicketResponse {
            assertEquals(TICKET_ID, ticketId)
            lastCompletionPayload = request
            return ticketResponse().copy(
                status = "awaiting_confirmation",
                version = 3,
                completionNote = request.completionNote,
                partsUsed = request.partsUsed,
                hasCompletionPhoto = true,
                completionSubmittedAt = "2026-08-16T10:15:00Z",
                updatedAt = "2026-08-16T10:15:00Z",
            )
        }
    }

    private companion object {
        const val TICKET_ID = "90000000-0000-0000-0000-000000000001"

        fun ticketResponse() = TicketResponse(
            id = TICKET_ID,
            clientRequestId = "50000000-0000-0000-0000-000000000001",
            propertyId = "20000000-0000-0000-0000-000000000001",
            unitId = "30000000-0000-0000-0000-000000000204",
            residentId = "10000000-0000-0000-0000-000000000001",
            title = "Bathroom pipe is leaking",
            description = "Water is collecting below the bathroom washbasin pipe.",
            category = "plumbing",
            urgencySuggestion = "soon",
            priority = "soon",
            accessWindow = "morning",
            status = "assigned",
            version = 2,
            assignedWorkerId = "40000000-0000-0000-0000-000000000001",
            assignedWorker = "Arun Kumar",
            residentFeedback = "The lower pipe joint is still dripping.",
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z",
        )
    }
}
