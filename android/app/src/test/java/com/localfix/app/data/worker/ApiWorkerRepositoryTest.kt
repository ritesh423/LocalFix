package com.localfix.app.data.worker

import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketCompletionPayload
import com.localfix.app.data.remote.TicketStartPayload
import com.localfix.app.data.remote.WorkerTicketApi
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiWorkerRepositoryTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-08-16T10:15:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun refreshMapsAssignedBackendTicketsIntoWorkerJobs() = runTest {
        val repository = ApiWorkerRepository(FakeWorkerApi(), clock)

        repository.refresh()

        val job = repository.workerData.value.jobs.single()
        assertEquals("Apartment A-204", job.unitLabel)
        assertEquals(ServiceCategory.PLUMBING, job.category)
        assertEquals(TicketStatus.ASSIGNED, job.status)
        assertEquals("Soon", job.priorityLabel)
        assertEquals("Updated 15 min ago", job.updatedLabel)
        assertEquals(WorkerSyncState.Ready, repository.syncState.value)
    }

    @Test
    fun startingAJobSendsItsVersionAndUpdatesTheLocalQueue() = runTest {
        val api = FakeWorkerApi()
        val repository = ApiWorkerRepository(api, clock)
        repository.refresh()

        repository.startJob(TICKET_ID, expectedVersion = 2)

        assertEquals(TicketStartPayload(expectedVersion = 2), api.lastStartPayload)
        val job = repository.workerData.value.jobs.single()
        assertEquals(TicketStatus.IN_PROGRESS, job.status)
        assertEquals(3, job.version)
        assertEquals("Updated just now", job.updatedLabel)
    }

    @Test
    fun completionSendsStructuredEvidenceAndUpdatesTheJob() = runTest {
        val api = FakeWorkerApi()
        val repository = ApiWorkerRepository(api, clock)
        repository.refresh()

        repository.submitCompletion(
            ticketId = TICKET_ID,
            expectedVersion = 2,
            completionNote = "Replaced the washer and tested the tap.",
            partsUsed = listOf("Rubber washer"),
            photoUri = "content://localfix/completion-photo",
        )

        assertEquals(
            TicketCompletionPayload(
                expectedVersion = 2,
                completionNote = "Replaced the washer and tested the tap.",
                partsUsed = listOf("Rubber washer"),
                photoUri = "content://localfix/completion-photo",
            ),
            api.lastCompletionPayload,
        )
        val job = repository.workerData.value.jobs.single()
        assertEquals(TicketStatus.AWAITING_CONFIRMATION, job.status)
        assertEquals("Replaced the washer and tested the tap.", job.completionNote)
        assertTrue(job.hasCompletionPhoto)
    }

    @Test
    fun failedFirstRefreshShowsAnErrorWithoutInventingJobs() = runTest {
        val repository = ApiWorkerRepository(
            ticketApi = FakeWorkerApi(listFailure = IOException("offline")),
            clock = clock,
        )

        repository.refresh()

        val state = repository.syncState.value as WorkerSyncState.Error
        assertFalse(state.hasPreviousResult)
        assertTrue(repository.workerData.value.jobs.isEmpty())
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
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z",
        )
    }
}
