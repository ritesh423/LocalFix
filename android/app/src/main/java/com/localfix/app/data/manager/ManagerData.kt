package com.localfix.app.data.manager

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.UrgencySuggestion

data class ManagerData(
    val propertyName: String,
    val tickets: List<ManagerTicket>,
    val workers: List<ManagerWorker>,
    val units: List<ManagerPropertyUnit>,
    val summary: ManagerSummary,
)

data class ManagerPropertyUnit(
    val id: String,
    val label: String,
)

data class ManagerResidentInvite(
    val inviteCode: String,
    val unitId: String,
    val unitLabel: String,
    val expiresAt: String,
)

data class ManagerSummary(
    val totalRequests: Int,
    val activeRequests: Int,
    val needsAssignment: Int,
    val assigned: Int,
    val inProgress: Int,
    val blocked: Int,
    val awaitingConfirmation: Int,
    val completed: Int,
) {
    companion object {
        val Empty = ManagerSummary(
            totalRequests = 0,
            activeRequests = 0,
            needsAssignment = 0,
            assigned = 0,
            inProgress = 0,
            blocked = 0,
            awaitingConfirmation = 0,
            completed = 0,
        )
    }
}

data class ManagerTicket(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val urgencySuggestion: UrgencySuggestion,
    val priority: ManagerPriority?,
    val accessWindow: AccessWindow,
    val status: TicketStatus,
    val version: Int,
    val assignedWorkerId: String?,
    val assignedWorker: String?,
    val updatedLabel: String,
    val commandDeliveryState: RequestDeliveryState = RequestDeliveryState.SYNCED,
    val commandFailureMessage: String? = null,
)

data class ManagerWorker(
    val id: String,
    val name: String,
    val specialty: ServiceCategory,
)

enum class ManagerPriority(val label: String) {
    ROUTINE("Routine"),
    SOON("Soon"),
    URGENT("Urgent"),
}
