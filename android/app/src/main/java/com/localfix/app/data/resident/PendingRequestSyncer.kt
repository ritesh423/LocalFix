package com.localfix.app.data.resident

import com.localfix.app.data.local.ResidentRequestStore
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketApiException
import kotlinx.coroutines.CancellationException
import java.io.IOException

class PendingRequestSyncer(
    private val ticketApi: TicketApi,
    private val residentRequestStore: ResidentRequestStore,
) {
    suspend fun sync(clientRequestId: String): PendingRequestSyncResult {
        val pendingRequest = residentRequestStore.getPendingRequest(clientRequestId)
            ?: return PendingRequestSyncResult.SUCCESS
        return try {
            val createdTicket = ticketApi.createTicket(pendingRequest.toPayload())
            residentRequestStore.completePendingRequest(
                clientRequestId = clientRequestId,
                ticket = createdTicket.toResidentTicketEntity(),
            )
            PendingRequestSyncResult.SUCCESS
        } catch (_: IOException) {
            PendingRequestSyncResult.RETRY
        } catch (error: TicketApiException) {
            if (error.statusCode == 408 || error.statusCode == 429 || error.statusCode >= 500) {
                PendingRequestSyncResult.RETRY
            } else {
                residentRequestStore.markRequestFailed(
                    clientRequestId,
                    "This request wasn't sent. Its details are still saved on this device.",
                )
                PendingRequestSyncResult.FAILURE
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            residentRequestStore.markRequestFailed(
                clientRequestId,
                "This request wasn't sent. Its details are still saved on this device.",
            )
            PendingRequestSyncResult.FAILURE
        }
    }
}

enum class PendingRequestSyncResult {
    SUCCESS,
    RETRY,
    FAILURE,
}
