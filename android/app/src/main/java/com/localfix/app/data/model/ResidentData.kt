package com.localfix.app.data.model

data class ResidentData(
    val account: ResidentAccount,
    val requests: List<MaintenanceRequest>,
    val serviceCategories: List<ServiceCategory>,
)

data class ResidentAccount(
    val name: String,
    val propertyName: String,
    val unitLabel: String,
    val phone: String,
    val email: String,
)

data class MaintenanceRequest(
    val id: String,
    val title: String,
    val category: ServiceCategory,
    val status: TicketStatus,
    val assignedWorker: String,
    val updatedLabel: String,
)

enum class ServiceCategory(val label: String) {
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    APPLIANCE("Appliance"),
    OTHER("Other"),
}

enum class TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    BLOCKED,
    AWAITING_CONFIRMATION,
    COMPLETED,
    CANCELLED,
}
