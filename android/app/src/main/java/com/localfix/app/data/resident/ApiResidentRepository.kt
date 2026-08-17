package com.localfix.app.data.resident

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.ResidentAccount
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.local.ResidentTicketDao
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketReviewPayload
import com.localfix.app.data.remote.TicketResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ApiResidentRepository(
    private val ticketApi: TicketApi,
    private val residentTicketDao: ResidentTicketDao,
    applicationScope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) : ResidentRepository {
    private val mutableRequestSyncState =
        MutableStateFlow<RequestSyncState>(RequestSyncState.InitialLoading)

    override val residentData: StateFlow<ResidentData> = residentTicketDao.observeTickets()
        .map { tickets ->
            emptyResidentData().copy(
                requests = tickets.map { it.toMaintenanceRequest(ticketApi, clock) },
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
        val hasCachedRequests = residentTicketDao.countTickets() > 0
        mutableRequestSyncState.value = if (hasCachedRequests) {
            RequestSyncState.Refreshing
        } else {
            RequestSyncState.InitialLoading
        }
        runCatching { ticketApi.listTickets() }
            .onSuccess { tickets ->
                residentTicketDao.replaceAllTickets(tickets.map(TicketResponse::toEntity))
                mutableRequestSyncState.value = RequestSyncState.Ready
            }
            .onFailure {
                mutableRequestSyncState.value = RequestSyncState.Error(
                    message = CONNECTION_ERROR,
                    hasPreviousResult = hasCachedRequests,
                )
            }
    }

    override suspend fun createRequest(request: NewMaintenanceRequest): String {
        return runCatching {
            ticketApi.createTicket(request.toPayload())
        }.onSuccess { createdTicket ->
            residentTicketDao.upsertTicket(createdTicket.toEntity())
        }.getOrThrow().id
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
        residentTicketDao.upsertTicket(reviewed.toEntity())
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

private fun NewMaintenanceRequest.toPayload(): TicketCreatePayload = TicketCreatePayload(
    clientRequestId = clientRequestId,
    title = title,
    description = description,
    category = category.name.lowercase(),
    urgencySuggestion = urgencySuggestion.name.lowercase(),
    accessWindow = accessWindow.name.lowercase(),
)

private fun TicketResponse.toEntity(): ResidentTicketEntity = ResidentTicketEntity(
    id = id,
    propertyId = propertyId.orEmpty(),
    unitId = unitId,
    residentId = residentId.orEmpty(),
    title = title,
    description = description,
    category = ServiceCategory.valueOf(category.uppercase()),
    status = TicketStatus.valueOf(status.uppercase()),
    urgencySuggestion = UrgencySuggestion.valueOf(urgencySuggestion.uppercase()),
    accessWindow = AccessWindow.valueOf(accessWindow.uppercase()),
    assignedWorker = assignedWorker,
    version = version,
    completionNote = completionNote,
    partsUsed = partsUsed,
    hasCompletionPhoto = hasCompletionPhoto,
    residentRating = residentRating,
    residentFeedback = residentFeedback,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

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
