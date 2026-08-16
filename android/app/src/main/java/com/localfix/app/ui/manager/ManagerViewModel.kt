package com.localfix.app.ui.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.manager.ManagerData
import com.localfix.app.data.manager.ManagerPriority
import com.localfix.app.data.manager.ManagerRepository
import com.localfix.app.data.manager.ManagerSyncState
import com.localfix.app.data.manager.ManagerTicket
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestStatusTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManagerViewModel(
    private val repository: ManagerRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(ManagerSelection())

    val uiState = combine(
        repository.managerData,
        repository.syncState,
        selection,
        ::createManagerUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createManagerUiState(
            repository.managerData.value,
            repository.syncState.value,
            selection.value,
        ),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun openTicket(ticketId: String) {
        selection.value = ManagerSelection(ticketId = ticketId)
    }

    fun selectPriority(priority: ManagerPriority) {
        selection.update { current ->
            current.copy(priority = priority, errorMessage = null)
        }
    }

    fun selectWorker(workerId: String) {
        selection.update { current ->
            current.copy(workerId = workerId, errorMessage = null)
        }
    }

    fun assignTicket() {
        val state = uiState.value.assignment
        val ticket = state.ticket ?: return
        val priority = state.selectedPriority ?: return
        val workerId = state.selectedWorkerId ?: return
        if (!state.canAssign) return

        selection.update { current ->
            current.copy(isAssigning = true, errorMessage = null)
        }
        viewModelScope.launch {
            runCatching {
                repository.assignTicket(
                    ticketId = ticket.id,
                    expectedVersion = ticket.version,
                    priority = priority,
                    workerId = workerId,
                )
            }.onSuccess {
                selection.update { current ->
                    current.copy(
                        isAssigning = false,
                        assignmentCompleted = true,
                    )
                }
            }.onFailure {
                selection.update { current ->
                    current.copy(
                        isAssigning = false,
                        errorMessage =
                            "Couldn't assign this request. Refresh the queue and try again.",
                    )
                }
            }
        }
    }

    fun consumeAssignmentCompleted() {
        selection.value = ManagerSelection()
    }

    companion object {
        fun factory(repository: ManagerRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ManagerViewModel(repository) }
            }
    }
}

private data class ManagerSelection(
    val ticketId: String? = null,
    val priority: ManagerPriority? = null,
    val workerId: String? = null,
    val isAssigning: Boolean = false,
    val errorMessage: String? = null,
    val assignmentCompleted: Boolean = false,
)

private fun createManagerUiState(
    data: ManagerData,
    syncState: ManagerSyncState,
    selection: ManagerSelection,
): ManagerUiState {
    val selectedTicket = data.tickets.find { it.id == selection.ticketId }
    return ManagerUiState(
        queue = ManagerQueueUiState(
            propertyName = data.propertyName,
            needsAssignmentCount = data.tickets.count { it.status == TicketStatus.OPEN },
            assignedCount = data.tickets.count { it.status == TicketStatus.ASSIGNED },
            tickets = data.tickets.map(ManagerTicket::toQueueItem),
            loadState = syncState.toLoadUiState(data.tickets.isNotEmpty()),
        ),
        assignment = ManagerAssignmentUiState(
            ticket = selectedTicket?.toAssignmentTicket(),
            workers = data.workers.map { worker ->
                ManagerWorkerItem(
                    id = worker.id,
                    name = worker.name,
                    specialty = worker.specialty,
                    specialtyLabel = worker.specialty.label,
                    isRecommended = worker.specialty == selectedTicket?.category,
                )
            },
            selectedPriority = selection.priority,
            selectedWorkerId = selection.workerId,
            isAssigning = selection.isAssigning,
            errorMessage = selection.errorMessage,
            assignmentCompleted = selection.assignmentCompleted,
        ),
    )
}

private fun ManagerSyncState.toLoadUiState(hasTickets: Boolean): RequestLoadUiState = when (this) {
    ManagerSyncState.InitialLoading -> RequestLoadUiState.Loading
    ManagerSyncState.Refreshing -> RequestLoadUiState.Refreshing
    ManagerSyncState.Ready -> if (hasTickets) {
        RequestLoadUiState.Content
    } else {
        RequestLoadUiState.Empty
    }
    is ManagerSyncState.Error -> if (hasPreviousResult) {
        RequestLoadUiState.Stale(message)
    } else {
        RequestLoadUiState.Failed(message)
    }
}

private fun ManagerTicket.toQueueItem() = ManagerTicketItem(
    id = id,
    reference = reference,
    unitLabel = unitLabel,
    title = title,
    categoryLabel = category.label,
    urgencyLabel = urgencySuggestion.label,
    statusLabel = status.label,
    statusTone = status.tone,
    assignedWorker = assignedWorker,
    updatedLabel = updatedLabel,
)

private fun ManagerTicket.toAssignmentTicket() = ManagerAssignmentTicket(
    id = id,
    reference = reference,
    unitLabel = unitLabel,
    title = title,
    description = description,
    categoryLabel = category.label,
    urgencyLabel = urgencySuggestion.label,
    accessWindowLabel = accessWindow.label,
    statusLabel = status.label,
    assignedWorker = assignedWorker,
    canBeAssigned = status == TicketStatus.OPEN,
    version = version,
)

private val TicketStatus.label: String
    get() = when (this) {
        TicketStatus.OPEN -> "Needs assignment"
        TicketStatus.ASSIGNED -> "Assigned"
        TicketStatus.IN_PROGRESS -> "In progress"
        TicketStatus.BLOCKED -> "Blocked"
        TicketStatus.AWAITING_CONFIRMATION -> "Awaiting confirmation"
        TicketStatus.COMPLETED -> "Completed"
        TicketStatus.CANCELLED -> "Cancelled"
    }

private val TicketStatus.tone: RequestStatusTone
    get() = when (this) {
        TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS ->
            RequestStatusTone.ACTIVE
        TicketStatus.BLOCKED, TicketStatus.AWAITING_CONFIRMATION ->
            RequestStatusTone.ATTENTION
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
