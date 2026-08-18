package com.localfix.app.data.resident

import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.ResidentAccount
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.local.PendingResidentRequestEntity
import com.localfix.app.data.local.ResidentRequestStore
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketReviewPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ApiResidentRepository(
    private val ticketApi: TicketApi,
    private val residentRequestStore: ResidentRequestStore,
    private val pendingRequestSyncScheduler: PendingRequestSyncScheduler,
    applicationScope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) : ResidentRepository {
    private val mutableRequestSyncState =
        MutableStateFlow<RequestSyncState>(RequestSyncState.InitialLoading)

    override val residentData: StateFlow<ResidentData> = combine(
        residentRequestStore.observeTickets(),
        residentRequestStore.observePendingRequests(),
    ) { tickets, pendingRequests ->
        emptyResidentData().copy(
            requests = pendingRequests.map { it.toMaintenanceRequest() } +
                tickets.map { it.toMaintenanceRequest(ticketApi, clock) },
        )
    }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyResidentData(),
        )
    override val requestSyncState: StateFlow<RequestSyncState> =
        mutableRequestSyncState

    override suspend fun refreshRequests() {
        val hasLocalRequests = residentRequestStore.hasLocalRequests()
        mutableRequestSyncState.value = if (hasLocalRequests) {
            RequestSyncState.Refreshing
        } else {
            RequestSyncState.InitialLoading
        }
        runCatching { ticketApi.listTickets() }
            .onSuccess { tickets ->
                residentRequestStore.replaceServerSnapshot(
                    tickets = tickets.map { it.toResidentTicketEntity() },
                    acknowledgedClientRequestIds = tickets.map { it.clientRequestId },
                )
                mutableRequestSyncState.value = RequestSyncState.Ready
            }
            .onFailure {
                mutableRequestSyncState.value = RequestSyncState.Error(
                    message = CONNECTION_ERROR,
                    hasPreviousResult = hasLocalRequests,
                )
            }
    }

    override suspend fun createRequest(request: NewMaintenanceRequest): String {
        residentRequestStore.queueRequest(request.toPendingEntity(clock))
        pendingRequestSyncScheduler.schedule(request.clientRequestId, replaceExisting = false)
        return request.clientRequestId
    }

    override suspend fun retryFailedRequest(clientRequestId: String) {
        val request = requireNotNull(
            residentRequestStore.getPendingRequest(clientRequestId),
        ) { "Pending request not found" }
        require(request.deliveryState == RequestDeliveryState.FAILED) {
            "Only failed requests can be retried manually"
        }
        residentRequestStore.markRequestPending(clientRequestId)
        runCatching {
            pendingRequestSyncScheduler.schedule(clientRequestId, replaceExisting = true)
        }.onFailure {
            residentRequestStore.markRequestFailed(
                clientRequestId,
                "The retry could not be scheduled. Your request is still saved.",
            )
        }.getOrThrow()
    }

    override suspend fun discardFailedRequest(clientRequestId: String) {
        val request = requireNotNull(
            residentRequestStore.getPendingRequest(clientRequestId),
        ) { "Pending request not found" }
        require(request.deliveryState == RequestDeliveryState.FAILED) {
            "Only failed requests can be discarded"
        }
        residentRequestStore.discardFailedRequest(clientRequestId)
    }

    override suspend fun reviewRequest(
        ticketId: String,
        expectedVersion: Int,
        decision: ResidentReviewDecision,
        rating: Int?,
        feedback: String?,
    ) {
        val reviewed = ticketApi.reviewTicket(
            ticketId = ticketId,
            request = TicketReviewPayload(
                expectedVersion = expectedVersion,
                decision = decision.name.lowercase(),
                rating = rating,
                feedback = feedback,
            ),
        )
        residentRequestStore.upsertTicket(reviewed.toResidentTicketEntity())
    }

    private companion object {
        const val CONNECTION_ERROR =
            "We couldn't reach LocalFix. Check your connection and try again."

        fun emptyResidentData() = ResidentData(
            account = ResidentAccount(
                name = "Ritesh",
                propertyName = "Lakeview Residency",
                unitLabel = "Apartment A-204",
                phone = "+91 98765 43210",
                email = "ritesh@example.com",
            ),
            requests = emptyList(),
            serviceCategories = ServiceCategory.entries,
        )
    }
}

private fun ResidentTicketEntity.toMaintenanceRequest(
    ticketApi: TicketApi,
    clock: Clock,
): MaintenanceRequest =
    MaintenanceRequest(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        urgencySuggestion = urgencySuggestion,
        accessWindow = accessWindow,
        assignedWorker = assignedWorker ?: "Awaiting assignment",
        updatedLabel = updatedAt.toUpdatedLabel(clock),
        version = version,
        completionNote = completionNote,
        partsUsed = partsUsed,
        completionPhotoUrl = if (hasCompletionPhoto) ticketApi.completionPhotoUrl(id) else null,
        residentRating = residentRating,
        residentFeedback = residentFeedback,
    )

private fun PendingResidentRequestEntity.toMaintenanceRequest(): MaintenanceRequest =
    MaintenanceRequest(
        id = clientRequestId,
        title = title,
        description = description,
        category = category,
        status = TicketStatus.OPEN,
        urgencySuggestion = urgencySuggestion,
        accessWindow = accessWindow,
        assignedWorker = if (deliveryState == RequestDeliveryState.FAILED) {
            failureMessage ?: "Request has not been sent"
        } else {
            "Will be available after the request is sent"
        },
        updatedLabel = if (deliveryState == RequestDeliveryState.FAILED) {
            "Not sent"
        } else {
            "Saved on this device"
        },
        photoUri = photoUri,
        deliveryState = deliveryState,
    )

private fun String.toUpdatedLabel(clock: Clock): String {
    val elapsed = runCatching {
        Duration.between(Instant.parse(this), clock.instant()).coerceAtLeast(Duration.ZERO)
    }.getOrDefault(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "Updated just now"
        elapsed.toHours() < 1 -> "Updated ${elapsed.toMinutes()} min ago"
        elapsed.toDays() < 1 -> "Updated ${elapsed.toHours()} hr ago"
        else -> "Updated ${elapsed.toDays()} days ago"
    }
}
