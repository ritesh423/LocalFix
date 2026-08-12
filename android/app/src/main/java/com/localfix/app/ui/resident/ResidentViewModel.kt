package com.localfix.app.ui.resident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.SavedRequestDraft
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.resident.RequestSyncState
import com.localfix.app.ui.home.MaintenanceRequestSummary
import com.localfix.app.ui.create.CreateRequestUiState
import com.localfix.app.ui.create.RequestDraft
import com.localfix.app.ui.create.RequestDraftErrors
import com.localfix.app.ui.home.ResidentHomeUiState
import com.localfix.app.ui.home.ServiceCategoryType
import com.localfix.app.ui.home.ServiceCategory as ServiceCategoryItem
import com.localfix.app.ui.profile.ResidentProfileUiState
import com.localfix.app.ui.requestdetail.ResidentRequestDetailUiState
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
    private var hasEditedDraft = false

    val createRequestState = mutableCreateRequestState.asStateFlow()

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
        ::createResidentUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createResidentUiState(
            repository.residentData.value,
            repository.requestSyncState.value,
            selectedFilter.value,
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
                    statusLabel = request.status.label,
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
            isLoadingRequests = requestSyncState is RequestSyncState.Loading,
            requestErrorMessage = (requestSyncState as? RequestSyncState.Error)?.message,
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
                    statusTone = request.status.tone,
                    statusLabel = request.status.label,
                    updatedLabel = request.updatedLabel,
                )
            },
            isLoading = requestSyncState is RequestSyncState.Loading,
            errorMessage = (requestSyncState as? RequestSyncState.Error)?.message,
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
            request.id to request.toDetailUiState()
        },
    )
}

private fun MaintenanceRequest.toDetailUiState(): ResidentRequestDetailUiState =
    ResidentRequestDetailUiState(
        id = reference,
        title = title,
        description = description,
        categoryLabel = category.label,
        statusLabel = status.label,
        statusTone = status.tone,
        urgencyLabel = urgencySuggestion.label,
        accessWindowLabel = accessWindow.label,
        assignedWorker = assignedWorker,
        updatedLabel = updatedLabel,
        photoUri = photoUri,
    )

private val MaintenanceRequest.reference: String
    get() = if (id.startsWith("LF-")) id else "LF-${id.take(8).uppercase()}"

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
