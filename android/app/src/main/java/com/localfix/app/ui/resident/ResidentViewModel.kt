package com.localfix.app.ui.resident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.SavedRequestDraft
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.resident.ResidentReviewDecision
import com.localfix.app.data.resident.RequestSyncState
import com.localfix.app.ui.home.MaintenanceRequestSummary
import com.localfix.app.ui.create.CreateRequestUiState
import com.localfix.app.ui.create.RequestDraft
import com.localfix.app.ui.create.RequestDraftErrors
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.home.ResidentHomeUiState
import com.localfix.app.ui.home.ServiceCategoryType
import com.localfix.app.ui.home.ServiceCategory as ServiceCategoryItem
import com.localfix.app.ui.profile.ResidentProfileUiState
import com.localfix.app.ui.requestdetail.ResidentRequestDetailUiState
import com.localfix.app.ui.requestdetail.RequestDeliveryUiState
import com.localfix.app.ui.requestdetail.ResidentReviewUiState
import com.localfix.app.ui.requests.RequestFilter
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.requests.ResidentRequestItem
import com.localfix.app.ui.requests.ResidentRequestsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResidentViewModel(
    private val repository: ResidentRepository,
    private val requestDraftRepository: RequestDraftRepository,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(RequestFilter.ALL)
    private val mutableCreateRequestState = MutableStateFlow(CreateRequestUiState())
    private val reviewState = MutableStateFlow(ResidentReviewState())
    private val deliveryActionState = MutableStateFlow(DeliveryActionState())
    private val mutableDiscardedRequestId = MutableStateFlow<String?>(null)
    private var hasEditedDraft = false

    val createRequestState = mutableCreateRequestState.asStateFlow()
    val discardedRequestId = mutableDiscardedRequestId.asStateFlow()

    init {
        viewModelScope.launch {
            val savedDraft = requestDraftRepository.observeDraft().first()
            if (!hasEditedDraft && savedDraft != null) {
                mutableCreateRequestState.update { state ->
                    state.copy(draft = savedDraft.toRequestDraft())
                }
            }
        }
        refreshRequests()
    }

    val uiState = combine(
        repository.residentData,
        repository.requestSyncState,
        selectedFilter,
        reviewState,
        deliveryActionState,
        ::createResidentUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createResidentUiState(
            repository.residentData.value,
            repository.requestSyncState.value,
            selectedFilter.value,
            reviewState.value,
            deliveryActionState.value,
        ),
    )

    fun selectRequestFilter(filter: RequestFilter) {
        selectedFilter.value = filter
    }

    fun refreshRequests() {
        viewModelScope.launch {
            repository.refreshRequests()
        }
    }

    fun openRequest(ticketId: String) {
        if (reviewState.value.ticketId != ticketId) {
            reviewState.value = ResidentReviewState(ticketId = ticketId)
        }
        if (deliveryActionState.value.ticketId != ticketId) {
            deliveryActionState.value = DeliveryActionState(ticketId = ticketId)
        }
    }

    fun retryFailedRequest(ticketId: String) {
        val ticket = repository.residentData.value.requests.find { it.id == ticketId } ?: return
        if (ticket.deliveryState != RequestDeliveryState.FAILED ||
            deliveryActionState.value.isWorking
        ) {
            return
        }
        deliveryActionState.value = DeliveryActionState(ticketId = ticketId, isWorking = true)
        viewModelScope.launch {
            runCatching { repository.retryFailedRequest(ticketId) }
                .onSuccess {
                    deliveryActionState.value = DeliveryActionState(ticketId = ticketId)
                }
                .onFailure {
                    deliveryActionState.value = DeliveryActionState(
                        ticketId = ticketId,
                        actionError = "Couldn't schedule the retry. Try again.",
                    )
                }
        }
    }

    fun discardFailedRequest(ticketId: String) {
        val ticket = repository.residentData.value.requests.find { it.id == ticketId } ?: return
        if (ticket.deliveryState != RequestDeliveryState.FAILED ||
            deliveryActionState.value.isWorking
        ) {
            return
        }
        deliveryActionState.value = DeliveryActionState(ticketId = ticketId, isWorking = true)
        viewModelScope.launch {
            runCatching { repository.discardFailedRequest(ticketId) }
                .onSuccess {
                    deliveryActionState.value = DeliveryActionState()
                    mutableDiscardedRequestId.value = ticketId
                }
                .onFailure {
                    deliveryActionState.value = DeliveryActionState(
                        ticketId = ticketId,
                        actionError = "Couldn't discard this request. Try again.",
                    )
                }
        }
    }

    fun consumeDiscardedRequest() {
        mutableDiscardedRequestId.value = null
    }

    fun selectReviewDecision(decision: ResidentReviewDecision) {
        reviewState.update { current ->
            current.copy(
                selectedDecision = decision,
                decisionError = null,
                ratingError = null,
                feedbackError = null,
                submissionError = null,
            )
        }
    }

    fun selectReviewRating(rating: Int) {
        reviewState.update { current ->
            current.copy(rating = rating, ratingError = null, submissionError = null)
        }
    }

    fun updateReviewFeedback(feedback: String) {
        reviewState.update { current ->
            current.copy(
                feedback = feedback.take(500),
                feedbackError = null,
                submissionError = null,
            )
        }
    }

    fun submitReview() {
        val current = reviewState.value
        val ticket = repository.residentData.value.requests.find {
            it.id == current.ticketId
        } ?: return
        if (ticket.status != TicketStatus.AWAITING_CONFIRMATION || current.isSubmitting) return

        val decisionError = if (current.selectedDecision == null) {
            "Choose whether the repair is complete"
        } else {
            null
        }
        val ratingError = if (
            current.selectedDecision == ResidentReviewDecision.CONFIRM && current.rating == null
        ) {
            "Choose a rating from 1 to 5"
        } else {
            null
        }
        val feedbackError = if (
            current.selectedDecision == ResidentReviewDecision.REQUEST_REWORK &&
            current.feedback.trim().length < 10
        ) {
            "Describe what still needs attention in at least 10 characters"
        } else {
            null
        }
        if (decisionError != null || ratingError != null || feedbackError != null) {
            reviewState.update {
                it.copy(
                    decisionError = decisionError,
                    ratingError = ratingError,
                    feedbackError = feedbackError,
                )
            }
            return
        }

        reviewState.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            runCatching {
                repository.reviewRequest(
                    ticketId = ticket.id,
                    expectedVersion = ticket.version,
                    decision = requireNotNull(current.selectedDecision),
                    rating = current.rating,
                    feedback = current.feedback.trim().ifBlank { null },
                )
            }.onSuccess {
                reviewState.update {
                    it.copy(isSubmitting = false, hasJustSubmitted = true)
                }
            }.onFailure {
                reviewState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionError =
                            "Couldn't send your review. Refresh the request and try again.",
                    )
                }
            }
        }
    }

    fun startRequestDraft(category: ServiceCategory? = null) {
        if (category == null) return
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(category = category),
                errors = state.errors.copy(category = null),
            )
        }
    }

    fun updateDraftCategory(category: ServiceCategory) {
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(category = category),
                errors = state.errors.copy(category = null),
            )
        }
    }

    fun updateDraftTitle(title: String) {
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(title = title.take(80)),
                errors = state.errors.copy(title = null),
            )
        }
    }

    fun updateDraftDescription(description: String) {
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(description = description.take(500)),
                errors = state.errors.copy(description = null),
            )
        }
    }

    fun updateDraftUrgency(urgency: UrgencySuggestion) {
        updateDraft { state ->
            state.copy(draft = state.draft.copy(urgencySuggestion = urgency))
        }
    }

    fun updateDraftAccessWindow(accessWindow: AccessWindow) {
        updateDraft { state ->
            state.copy(draft = state.draft.copy(accessWindow = accessWindow))
        }
    }

    fun updateDraftPhoto(photoUri: String) {
        if (photoUri.isBlank()) {
            reportPhotoSelectionFailure()
            return
        }
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(photoUri = photoUri),
                photoError = null,
            )
        }
    }

    fun removeDraftPhoto() {
        updateDraft { state ->
            state.copy(
                draft = state.draft.copy(photoUri = null),
                photoError = null,
            )
        }
    }

    fun reportPhotoSelectionFailure() {
        mutableCreateRequestState.update { state ->
            state.copy(photoError = "Couldn't keep access to that photo. Choose another one.")
        }
    }

    fun submitRequestDraft() {
        val draft = mutableCreateRequestState.value.draft
        val errors = draft.validate()
        if (errors.hasErrors) {
            mutableCreateRequestState.update { state -> state.copy(errors = errors) }
            return
        }

        mutableCreateRequestState.update { state ->
            state.copy(isSubmitting = true, submissionError = null)
        }
        viewModelScope.launch {
            runCatching {
                val requestId = repository.createRequest(draft.toNewRequest())
                requestDraftRepository.clearDraft()
                requestId
            }.onSuccess { requestId ->
                selectedFilter.value = RequestFilter.ALL
                mutableCreateRequestState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        submittedRequestId = requestId,
                    )
                }
            }.onFailure {
                mutableCreateRequestState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        submissionError = "Couldn't save the request. Try again.",
                    )
                }
            }
        }
    }

    fun consumeRequestSubmission() {
        mutableCreateRequestState.value = CreateRequestUiState()
    }

    fun discardRequestDraft() {
        mutableCreateRequestState.value = CreateRequestUiState()
        hasEditedDraft = true
        viewModelScope.launch {
            requestDraftRepository.clearDraft()
        }
    }

    private fun updateDraft(transform: (CreateRequestUiState) -> CreateRequestUiState) {
        mutableCreateRequestState.update(transform)
        hasEditedDraft = true
        val draft = mutableCreateRequestState.value.draft.toSavedDraft()
        viewModelScope.launch {
            requestDraftRepository.saveDraft(draft)
        }
    }

    companion object {
        fun factory(
            repository: ResidentRepository,
            requestDraftRepository: RequestDraftRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ResidentViewModel(repository, requestDraftRepository)
            }
        }
    }
}

private fun SavedRequestDraft.toRequestDraft(): RequestDraft = RequestDraft(
    clientRequestId = clientRequestId,
    category = category,
    title = title,
    description = description,
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
)

private fun RequestDraft.toSavedDraft(): SavedRequestDraft = SavedRequestDraft(
    clientRequestId = clientRequestId,
    category = category,
    title = title,
    description = description,
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
)

private fun RequestDraft.validate(): RequestDraftErrors = RequestDraftErrors(
    category = if (category == null) "Choose a service category" else null,
    title = if (title.trim().length < 5) "Enter at least 5 characters" else null,
    description = if (description.trim().length < 10) {
        "Describe the problem in at least 10 characters"
    } else {
        null
    },
)

private fun RequestDraft.toNewRequest(): NewMaintenanceRequest = NewMaintenanceRequest(
    clientRequestId = clientRequestId,
    title = title.trim(),
    description = description.trim(),
    category = requireNotNull(category),
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
)

data class ResidentUiState(
    val home: ResidentHomeUiState,
    val requests: ResidentRequestsUiState,
    val profile: ResidentProfileUiState,
    val requestDetails: Map<String, ResidentRequestDetailUiState>,
)

private fun createResidentUiState(
    data: ResidentData,
    requestSyncState: RequestSyncState,
    selectedFilter: RequestFilter,
    reviewState: ResidentReviewState,
    deliveryActionState: DeliveryActionState,
): ResidentUiState {
    val visibleRequests = data.requests.filter { request ->
        when (selectedFilter) {
            RequestFilter.ALL -> true
            RequestFilter.ACTIVE -> request.status !in terminalStatuses
            RequestFilter.COMPLETED -> request.status == TicketStatus.COMPLETED
        }
    }
    val activeRequest = data.requests.firstOrNull { request ->
        request.status !in terminalStatuses && request.status != TicketStatus.AWAITING_CONFIRMATION
    }
    val requestLoadState = requestSyncState.toLoadUiState(
        hasVisibleRequests = visibleRequests.isNotEmpty(),
    )

    return ResidentUiState(
        home = ResidentHomeUiState(
            residentName = data.account.name,
            propertyName = data.account.propertyName,
            unitLabel = data.account.unitLabel,
            activeRequestCount = data.requests.count { request ->
                request.status !in terminalStatuses &&
                    request.status != TicketStatus.AWAITING_CONFIRMATION
            },
            awaitingConfirmationCount = data.requests.count { request ->
                request.status == TicketStatus.AWAITING_CONFIRMATION
            },
            activeRequest = activeRequest?.let { request ->
                MaintenanceRequestSummary(
                    id = request.id,
                    reference = request.reference,
                    title = request.title,
                    statusLabel = request.displayStatusLabel,
                    statusTone = request.displayStatusTone,
                    assignedWorker = request.assignedWorker,
                    updatedLabel = request.updatedLabel,
                )
            },
            categories = data.serviceCategories.map { category ->
                ServiceCategoryItem(
                    type = category.toUiType(),
                    label = category.label,
                )
            },
            requestLoadState = requestSyncState.toLoadUiState(
                hasVisibleRequests = data.requests.isNotEmpty(),
            ),
        ),
        requests = ResidentRequestsUiState(
            unitLabel = data.account.unitLabel,
            selectedFilter = selectedFilter,
            requests = visibleRequests.map { request ->
                ResidentRequestItem(
                    id = request.id,
                    reference = request.reference,
                    title = request.title,
                    category = request.category.label,
                    statusTone = request.displayStatusTone,
                    statusLabel = request.displayStatusLabel,
                    updatedLabel = request.updatedLabel,
                )
            },
            requestLoadState = requestLoadState,
        ),
        profile = ResidentProfileUiState(
            name = data.account.name,
            statusLabel = "Resident · Active",
            propertyName = data.account.propertyName,
            unitLabel = data.account.unitLabel,
            phone = data.account.phone,
            email = data.account.email,
        ),
        requestDetails = data.requests.associate { request ->
            request.id to request.toDetailUiState(
                reviewState.takeIf { it.ticketId == request.id },
                deliveryActionState.takeIf { it.ticketId == request.id },
            )
        },
    )
}

private fun RequestSyncState.toLoadUiState(
    hasVisibleRequests: Boolean,
): RequestLoadUiState = when (this) {
    RequestSyncState.InitialLoading -> RequestLoadUiState.Loading
    RequestSyncState.Refreshing -> RequestLoadUiState.Refreshing
    RequestSyncState.Ready -> if (hasVisibleRequests) {
        RequestLoadUiState.Content
    } else {
        RequestLoadUiState.Empty
    }
    is RequestSyncState.Error -> if (hasPreviousResult || hasVisibleRequests) {
        RequestLoadUiState.Stale(message)
    } else {
        RequestLoadUiState.Failed(message)
    }
}

private fun MaintenanceRequest.toDetailUiState(
    reviewState: ResidentReviewState?,
    deliveryActionState: DeliveryActionState?,
): ResidentRequestDetailUiState =
    ResidentRequestDetailUiState(
        requestId = id,
        id = reference,
        title = title,
        description = description,
        categoryLabel = category.label,
        statusLabel = displayStatusLabel,
        statusTone = displayStatusTone,
        urgencyLabel = urgencySuggestion.label,
        accessWindowLabel = accessWindow.label,
        assignedWorker = assignedWorker,
        updatedLabel = updatedLabel,
        photoUri = photoUri,
        completionNote = completionNote,
        partsUsed = partsUsed,
        completionPhotoUrl = completionPhotoUrl,
        residentRating = residentRating,
        residentFeedback = residentFeedback,
        canReview = status == TicketStatus.AWAITING_CONFIRMATION,
        delivery = toDeliveryUiState(deliveryActionState),
        review = reviewState?.toUiState() ?: ResidentReviewUiState(),
    )

private fun MaintenanceRequest.toDeliveryUiState(
    actionState: DeliveryActionState?,
): RequestDeliveryUiState? = when (deliveryState) {
    RequestDeliveryState.SYNCED -> null
    RequestDeliveryState.PENDING -> RequestDeliveryUiState(
        title = "Waiting to send",
        message = "This request is saved on your device and will send automatically when connected.",
        canRetry = false,
        canDiscard = false,
    )
    RequestDeliveryState.FAILED -> RequestDeliveryUiState(
        title = "Request wasn't sent",
        message = assignedWorker,
        canRetry = true,
        canDiscard = true,
        isWorking = actionState?.isWorking == true,
        actionError = actionState?.actionError,
    )
}

private data class ResidentReviewState(
    val ticketId: String? = null,
    val selectedDecision: ResidentReviewDecision? = null,
    val rating: Int? = null,
    val feedback: String = "",
    val decisionError: String? = null,
    val ratingError: String? = null,
    val feedbackError: String? = null,
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val hasJustSubmitted: Boolean = false,
)

private data class DeliveryActionState(
    val ticketId: String? = null,
    val isWorking: Boolean = false,
    val actionError: String? = null,
)

private fun ResidentReviewState.toUiState() = ResidentReviewUiState(
    selectedDecision = selectedDecision,
    rating = rating,
    feedback = feedback,
    decisionError = decisionError,
    ratingError = ratingError,
    feedbackError = feedbackError,
    isSubmitting = isSubmitting,
    submissionError = submissionError,
    hasJustSubmitted = hasJustSubmitted,
)

private val MaintenanceRequest.reference: String
    get() = when {
        id.startsWith("LF-") -> id
        deliveryState != RequestDeliveryState.SYNCED -> "LOCAL-${id.take(8).uppercase()}"
        else -> "LF-${id.take(8).uppercase()}"
    }

private val MaintenanceRequest.displayStatusLabel: String
    get() = when (deliveryState) {
        RequestDeliveryState.PENDING -> "Waiting to send"
        RequestDeliveryState.FAILED -> "Send failed"
        RequestDeliveryState.SYNCED -> status.label
    }

private val MaintenanceRequest.displayStatusTone: RequestStatusTone
    get() = when (deliveryState) {
        RequestDeliveryState.PENDING -> RequestStatusTone.NEUTRAL
        RequestDeliveryState.FAILED -> RequestStatusTone.ATTENTION
        RequestDeliveryState.SYNCED -> status.tone
    }

private val terminalStatuses = setOf(
    TicketStatus.COMPLETED,
    TicketStatus.CANCELLED,
)

private val TicketStatus.label: String
    get() = when (this) {
        TicketStatus.OPEN -> "Open"
        TicketStatus.ASSIGNED -> "Assigned"
        TicketStatus.IN_PROGRESS -> "In progress"
        TicketStatus.BLOCKED -> "Blocked"
        TicketStatus.AWAITING_CONFIRMATION -> "Confirm repair"
        TicketStatus.COMPLETED -> "Completed"
        TicketStatus.CANCELLED -> "Cancelled"
    }

private val TicketStatus.tone: RequestStatusTone
    get() = when (this) {
        TicketStatus.OPEN,
        TicketStatus.ASSIGNED,
        TicketStatus.IN_PROGRESS,
        -> RequestStatusTone.ACTIVE
        TicketStatus.BLOCKED,
        TicketStatus.AWAITING_CONFIRMATION,
        -> RequestStatusTone.ATTENTION
        TicketStatus.COMPLETED -> RequestStatusTone.COMPLETED
        TicketStatus.CANCELLED -> RequestStatusTone.NEUTRAL
    }

private val UrgencySuggestion.label: String
    get() = when (this) {
        UrgencySuggestion.ROUTINE -> "Routine"
        UrgencySuggestion.SOON -> "Soon"
        UrgencySuggestion.URGENT -> "Urgent"
    }

private val AccessWindow.label: String
    get() = when (this) {
        AccessWindow.ANYTIME -> "Any time today"
        AccessWindow.MORNING -> "Morning · 8 AM–12 PM"
        AccessWindow.AFTERNOON -> "Afternoon · 12–4 PM"
        AccessWindow.EVENING -> "Evening · 4–8 PM"
    }

private fun ServiceCategory.toUiType(): ServiceCategoryType = when (this) {
    ServiceCategory.PLUMBING -> ServiceCategoryType.PLUMBING
    ServiceCategory.ELECTRICAL -> ServiceCategoryType.ELECTRICAL
    ServiceCategory.APPLIANCE -> ServiceCategoryType.APPLIANCE
    ServiceCategory.OTHER -> ServiceCategoryType.OTHER
}
