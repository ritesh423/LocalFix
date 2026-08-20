package com.localfix.app.data.resident

import com.localfix.app.data.local.PendingResidentReviewEntity
import com.localfix.app.data.local.ResidentRequestStore
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketApiException
import kotlinx.coroutines.CancellationException
import java.io.IOException

class PendingReviewSyncer(
    private val ticketApi: TicketApi,
    private val residentRequestStore: ResidentRequestStore,
) {
    suspend fun sync(ticketId: String): PendingRequestSyncResult {
        val pendingReview = residentRequestStore.getPendingReview(ticketId)
            ?: return PendingRequestSyncResult.SUCCESS
        return try {
            val reviewedTicket = ticketApi.reviewTicket(ticketId, pendingReview.toPayload())
            complete(pendingReview, reviewedTicket.toResidentTicketEntity())
        } catch (_: IOException) {
            PendingRequestSyncResult.RETRY
        } catch (error: TicketApiException) {
            when {
                error.statusCode == 409 -> resolvePossibleDuplicate(pendingReview)
                error.isRetryable -> PendingRequestSyncResult.RETRY
                else -> fail(pendingReview.ticketId)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            fail(pendingReview.ticketId)
        }
    }

    private suspend fun resolvePossibleDuplicate(
        pendingReview: PendingResidentReviewEntity,
    ): PendingRequestSyncResult = try {
        val serverTicket = ticketApi.listTickets().find { it.id == pendingReview.ticketId }
        if (serverTicket != null && serverTicket.matches(pendingReview)) {
            complete(pendingReview, serverTicket.toResidentTicketEntity())
        } else {
            fail(pendingReview.ticketId)
        }
    } catch (_: IOException) {
        PendingRequestSyncResult.RETRY
    } catch (error: TicketApiException) {
        if (error.isRetryable) PendingRequestSyncResult.RETRY else fail(pendingReview.ticketId)
    }

    private suspend fun complete(
        pendingReview: PendingResidentReviewEntity,
        ticket: ResidentTicketEntity,
    ): PendingRequestSyncResult {
        residentRequestStore.completePendingReview(pendingReview.ticketId, ticket)
        return PendingRequestSyncResult.SUCCESS
    }

    private suspend fun fail(ticketId: String): PendingRequestSyncResult {
        residentRequestStore.markReviewFailed(
            ticketId,
            "This review wasn't sent. Check the latest request and submit it again.",
        )
        return PendingRequestSyncResult.FAILURE
    }

    private val TicketApiException.isRetryable: Boolean
        get() = statusCode == 408 || statusCode == 429 || statusCode >= 500
}
