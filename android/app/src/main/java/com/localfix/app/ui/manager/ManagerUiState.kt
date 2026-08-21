package com.localfix.app.ui.manager

import com.localfix.app.data.manager.ManagerPriority
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestStatusTone

data class ManagerUiState(
    val queue: ManagerQueueUiState,
    val assignment: ManagerAssignmentUiState,
)

data class ManagerQueueUiState(
    val propertyName: String,
    val summary: ManagerSummaryUiState,
    val tickets: List<ManagerTicketItem>,
    val loadState: RequestLoadUiState,
)

data class ManagerSummaryUiState(
    val activeRequests: Int,
    val needsAssignment: Int,
    val assigned: Int,
    val inProgress: Int,
    val blocked: Int,
    val awaitingConfirmation: Int,
    val completed: Int,
)

data class ManagerTicketItem(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val categoryLabel: String,
    val urgencyLabel: String,
    val statusLabel: String,
    val statusTone: RequestStatusTone,
    val assignedWorker: String?,
    val updatedLabel: String,
)

data class ManagerAssignmentUiState(
    val ticket: ManagerAssignmentTicket? = null,
    val workers: List<ManagerWorkerItem> = emptyList(),
    val selectedPriority: ManagerPriority? = null,
    val selectedWorkerId: String? = null,
    val isAssigning: Boolean = false,
    val errorMessage: String? = null,
    val assignmentCompleted: Boolean = false,
) {
    val canAssign: Boolean
        get() = ticket?.canBeAssigned == true &&
            selectedPriority != null &&
            selectedWorkerId != null &&
            !isAssigning
}

data class ManagerAssignmentTicket(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val urgencyLabel: String,
    val accessWindowLabel: String,
    val statusLabel: String,
    val assignedWorker: String?,
    val canBeAssigned: Boolean,
    val version: Int,
)

data class ManagerWorkerItem(
    val id: String,
    val name: String,
    val specialty: ServiceCategory,
    val specialtyLabel: String,
    val isRecommended: Boolean,
)
