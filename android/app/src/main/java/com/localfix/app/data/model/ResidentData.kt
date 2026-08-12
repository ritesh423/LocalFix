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
    val description: String,
    val category: ServiceCategory,
    val status: TicketStatus,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val assignedWorker: String,
    val updatedLabel: String,
    val photoUri: String? = null,
)

data class NewMaintenanceRequest(
    val clientRequestId: String,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val photoUri: String? = null,
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

enum class UrgencySuggestion {
    ROUTINE,
    SOON,
    URGENT,
}

enum class AccessWindow {
    ANYTIME,
    MORNING,
    AFTERNOON,
    EVENING,
}
