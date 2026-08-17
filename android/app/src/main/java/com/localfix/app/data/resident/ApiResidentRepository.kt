package com.localfix.app.data.resident

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.ResidentAccount
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.remote.TicketApi
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketReviewPayload
import com.localfix.app.data.remote.TicketResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ApiResidentRepository(
    private val ticketApi: TicketApi,
    private val clock: Clock = Clock.systemUTC(),
) : ResidentRepository {
    private val mutableResidentData = MutableStateFlow(emptyResidentData())
    private val mutableRequestSyncState =
        MutableStateFlow<RequestSyncState>(RequestSyncState.InitialLoading)
    private var hasLoadedRequests = false

    override val residentData: StateFlow<ResidentData> = mutableResidentData.asStateFlow()
    override val requestSyncState: StateFlow<RequestSyncState> =
        mutableRequestSyncState.asStateFlow()

    override suspend fun refreshRequests() {
        mutableRequestSyncState.value = if (hasLoadedRequests) {
            RequestSyncState.Refreshing
        } else {
            RequestSyncState.InitialLoading
        }
        runCatching { ticketApi.listTickets() }
            .onSuccess { tickets ->
                mutableResidentData.update { data ->
                    data.copy(
                        requests = tickets.map {
                            it.toMaintenanceRequest(
                                clock = clock,
                                completionPhotoUrl = if (it.hasCompletionPhoto) {
                                    ticketApi.completionPhotoUrl(it.id)
                                } else {
                                    null
                                },
                            )
                        },
                    )
                }
                hasLoadedRequests = true
                mutableRequestSyncState.value = RequestSyncState.Ready
            }
            .onFailure {
                mutableRequestSyncState.value = RequestSyncState.Error(
                    message = CONNECTION_ERROR,
                    hasPreviousResult = hasLoadedRequests,
                )
            }
    }

    override suspend fun createRequest(request: NewMaintenanceRequest): String {
        return runCatching {
            ticketApi.createTicket(request.toPayload())
        }.onSuccess { createdTicket ->
            val requestModel = createdTicket.toMaintenanceRequest(clock, null)
            mutableResidentData.update { data ->
                data.copy(
                    requests = listOf(requestModel) + data.requests.filterNot { existing ->
                        existing.id == requestModel.id
                    },
                )
            }
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
        val requestModel = reviewed.toMaintenanceRequest(
            clock = clock,
            completionPhotoUrl = if (reviewed.hasCompletionPhoto) {
                ticketApi.completionPhotoUrl(reviewed.id)
            } else {
                null
            },
        )
        mutableResidentData.update { data ->
            data.copy(
                requests = data.requests.map { existing ->
                    if (existing.id == requestModel.id) requestModel else existing
                },
            )
        }
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

private fun TicketResponse.toMaintenanceRequest(
    clock: Clock,
    completionPhotoUrl: String?,
): MaintenanceRequest =
    MaintenanceRequest(
        id = id,
        title = title,
        description = description,
        category = ServiceCategory.valueOf(category.uppercase()),
        status = TicketStatus.valueOf(status.uppercase()),
        urgencySuggestion = UrgencySuggestion.valueOf(urgencySuggestion.uppercase()),
        accessWindow = AccessWindow.valueOf(accessWindow.uppercase()),
        assignedWorker = assignedWorker ?: "Awaiting assignment",
        updatedLabel = updatedAt.toUpdatedLabel(clock),
        version = version,
        completionNote = completionNote,
        partsUsed = partsUsed,
        completionPhotoUrl = completionPhotoUrl,
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
