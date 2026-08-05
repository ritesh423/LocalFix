package com.localfix.app.ui.resident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.ui.home.MaintenanceRequestSummary
import com.localfix.app.ui.create.CreateRequestUiState
import com.localfix.app.ui.create.RequestDraft
import com.localfix.app.ui.create.RequestDraftErrors
import com.localfix.app.ui.home.ResidentHomeUiState
import com.localfix.app.ui.home.ServiceCategoryType
import com.localfix.app.ui.home.ServiceCategory as ServiceCategoryItem
import com.localfix.app.ui.profile.ResidentProfileUiState
import com.localfix.app.ui.requests.RequestFilter
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.requests.ResidentRequestItem
import com.localfix.app.ui.requests.ResidentRequestsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResidentViewModel(
    private val repository: ResidentRepository,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(RequestFilter.ALL)
    private val mutableCreateRequestState = MutableStateFlow(CreateRequestUiState())

    val createRequestState = mutableCreateRequestState.asStateFlow()

    val uiState = combine(
        repository.residentData,
        selectedFilter,
        ::createResidentUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createResidentUiState(
            repository.residentData.value,
            selectedFilter.value,
        ),
    )

    fun selectRequestFilter(filter: RequestFilter) {
        selectedFilter.value = filter
    }

    fun startRequestDraft(category: ServiceCategory? = null) {
        if (category == null) return
        mutableCreateRequestState.update { state ->
            state.copy(
                draft = state.draft.copy(category = category),
                errors = state.errors.copy(category = null),
            )
        }
    }

    fun updateDraftCategory(category: ServiceCategory) {
        mutableCreateRequestState.update { state ->
            state.copy(
                draft = state.draft.copy(category = category),
                errors = state.errors.copy(category = null),
            )
        }
    }

    fun updateDraftTitle(title: String) {
        mutableCreateRequestState.update { state ->
            state.copy(
                draft = state.draft.copy(title = title.take(80)),
                errors = state.errors.copy(title = null),
            )
        }
    }

    fun updateDraftDescription(description: String) {
        mutableCreateRequestState.update { state ->
            state.copy(
                draft = state.draft.copy(description = description.take(500)),
                errors = state.errors.copy(description = null),
            )
        }
    }

    fun updateDraftUrgency(urgency: UrgencySuggestion) {
        mutableCreateRequestState.update { state ->
            state.copy(draft = state.draft.copy(urgencySuggestion = urgency))
        }
    }

    fun updateDraftAccessWindow(accessWindow: AccessWindow) {
        mutableCreateRequestState.update { state ->
            state.copy(draft = state.draft.copy(accessWindow = accessWindow))
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
                repository.createRequest(draft.toNewRequest())
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
    }

    companion object {
        fun factory(repository: ResidentRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ResidentViewModel(repository)
            }
        }
    }
}

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
    title = title.trim(),
    description = description.trim(),
    category = requireNotNull(category),
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
)

data class ResidentUiState(
    val home: ResidentHomeUiState,
    val requests: ResidentRequestsUiState,
    val profile: ResidentProfileUiState,
)

private fun createResidentUiState(
    data: ResidentData,
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
        ),
        requests = ResidentRequestsUiState(
            unitLabel = data.account.unitLabel,
            selectedFilter = selectedFilter,
            requests = visibleRequests.map { request ->
                ResidentRequestItem(
                    id = request.id,
                    title = request.title,
                    category = request.category.label,
                    statusTone = request.status.tone,
                    statusLabel = request.status.label,
                    updatedLabel = request.updatedLabel,
                )
            },
        ),
        profile = ResidentProfileUiState(
            name = data.account.name,
            statusLabel = "Resident · Active",
            propertyName = data.account.propertyName,
            unitLabel = data.account.unitLabel,
            phone = data.account.phone,
            email = data.account.email,
        ),
    )
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

private fun ServiceCategory.toUiType(): ServiceCategoryType = when (this) {
    ServiceCategory.PLUMBING -> ServiceCategoryType.PLUMBING
    ServiceCategory.ELECTRICAL -> ServiceCategoryType.ELECTRICAL
    ServiceCategory.APPLIANCE -> ServiceCategoryType.APPLIANCE
    ServiceCategory.OTHER -> ServiceCategoryType.OTHER
}
