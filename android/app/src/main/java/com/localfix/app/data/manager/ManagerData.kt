package com.localfix.app.data.manager

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion

data class ManagerData(
    val propertyName: String,
    val tickets: List<ManagerTicket>,
    val workers: List<ManagerWorker>,
)

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
