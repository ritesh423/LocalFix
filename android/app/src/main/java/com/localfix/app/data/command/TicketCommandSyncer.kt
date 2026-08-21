package com.localfix.app.data.command

import com.localfix.app.data.local.PendingTicketCommandEntity
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.remote.ManagerTicketApi
import com.localfix.app.data.remote.TicketApiException
import com.localfix.app.data.remote.TicketAssignmentPayload
import com.localfix.app.data.remote.TicketCompletionPayload
import com.localfix.app.data.remote.TicketResponse
import com.localfix.app.data.remote.TicketStartPayload
import com.localfix.app.data.remote.WorkerTicketApi
import com.localfix.app.data.resident.PendingRequestSyncResult
import java.io.IOException
import kotlinx.coroutines.CancellationException

class TicketCommandSyncer(
    private val managerApi: ManagerTicketApi,
    private val workerApi: WorkerTicketApi,
    private val store: TicketCommandStore,
    private val onAssignmentSynced: (TicketResponse) -> Unit,
    private val onStartSynced: (TicketResponse) -> Unit,
    private val onCompletionSynced: (TicketResponse) -> Unit = {},
    private val onCompletionPhotoReleased: (String) -> Unit = {},
) {
    suspend fun sync(
        ticketId: String,
        commandType: TicketCommandType,
    ): PendingRequestSyncResult {
        val command = store.getCommand(ticketId, commandType)
            ?: return PendingRequestSyncResult.SUCCESS
        return try {
            complete(command, execute(command))
        } catch (_: IOException) {
            PendingRequestSyncResult.RETRY
        } catch (error: TicketApiException) {
            when {
                error.statusCode == 409 -> resolvePossibleDuplicate(command)
                error.isRetryable -> PendingRequestSyncResult.RETRY
                else -> fail(command)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            fail(command)
        }
    }

    private suspend fun execute(command: PendingTicketCommandEntity): TicketResponse =
        when (command.commandType) {
            TicketCommandType.ASSIGN -> managerApi.assignTicket(
                ticketId = command.ticketId,
                request = TicketAssignmentPayload(
                    expectedVersion = command.expectedVersion,
                    priority = requireNotNull(command.priority),
                    workerId = requireNotNull(command.workerId),
                ),
            )
            TicketCommandType.START -> workerApi.startTicket(
                ticketId = command.ticketId,
                request = TicketStartPayload(command.expectedVersion),
            )
            TicketCommandType.COMPLETE -> workerApi.submitCompletion(
                ticketId = command.ticketId,
                request = TicketCompletionPayload(
                    expectedVersion = command.expectedVersion,
                    completionNote = requireNotNull(command.completionNote),
                    partsUsed = command.partsUsed.orEmpty(),
                    photoUri = requireNotNull(command.photoUri),
                ),
            )
        }

    private suspend fun resolvePossibleDuplicate(
        command: PendingTicketCommandEntity,
    ): PendingRequestSyncResult = try {
        val ticket = when (command.commandType) {
            TicketCommandType.ASSIGN -> managerApi.listManagerTickets()
            TicketCommandType.START, TicketCommandType.COMPLETE ->
                workerApi.listWorkerTickets()
        }.find { it.id == command.ticketId }
        if (ticket != null && ticket.matches(command)) {
            complete(command, ticket)
        } else {
            fail(command)
        }
    } catch (_: IOException) {
        PendingRequestSyncResult.RETRY
    } catch (error: TicketApiException) {
        if (error.isRetryable) PendingRequestSyncResult.RETRY else fail(command)
    }

    private suspend fun complete(
        command: PendingTicketCommandEntity,
        ticket: TicketResponse,
    ): PendingRequestSyncResult {
        when (command.commandType) {
            TicketCommandType.ASSIGN -> onAssignmentSynced(ticket)
            TicketCommandType.START -> onStartSynced(ticket)
            TicketCommandType.COMPLETE -> {
                onCompletionSynced(ticket)
                command.photoUri?.let(onCompletionPhotoReleased)
            }
        }
        store.complete(command.ticketId, command.commandType)
        return PendingRequestSyncResult.SUCCESS
    }

    private suspend fun fail(command: PendingTicketCommandEntity): PendingRequestSyncResult {
        store.markFailed(
            command.ticketId,
            command.commandType,
            if (command.commandType == TicketCommandType.COMPLETE) {
                "The completion couldn't be sent. Check the photo and try again."
            } else {
                "This action wasn't sent. Refresh the ticket and try again."
            },
        )
        return PendingRequestSyncResult.FAILURE
    }

    private fun TicketResponse.matches(command: PendingTicketCommandEntity): Boolean =
        version > command.expectedVersion && when (command.commandType) {
            TicketCommandType.ASSIGN ->
                assignedWorkerId == command.workerId && priority == command.priority
            TicketCommandType.START -> status in setOf(
                "in_progress",
                "blocked",
                "awaiting_confirmation",
                "completed",
            )
            TicketCommandType.COMPLETE -> status in setOf(
                "awaiting_confirmation",
                "completed",
            )
        }

    private val TicketApiException.isRetryable: Boolean
        get() = statusCode == 408 || statusCode == 429 || statusCode >= 500
}
