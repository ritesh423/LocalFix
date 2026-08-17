package com.localfix.app.ui.home

import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestStatusTone

data class ResidentHomeUiState(
    val residentName: String,
    val propertyName: String,
    val unitLabel: String,
    val activeRequestCount: Int,
    val awaitingConfirmationCount: Int,
    val activeRequest: MaintenanceRequestSummary?,
    val categories: List<ServiceCategory>,
    val requestLoadState: RequestLoadUiState = RequestLoadUiState.Content,
) {
    companion object {
        val sample = ResidentHomeUiState(
            residentName = "Ritesh",
            propertyName = "Lakeview Residency",
            unitLabel = "Apartment A-204",
            activeRequestCount = 1,
            awaitingConfirmationCount = 0,
            activeRequest = MaintenanceRequestSummary(
                id = "LF-1042",
                title = "Leaking kitchen tap",
                statusLabel = "In progress",
                statusTone = RequestStatusTone.ACTIVE,
                assignedWorker = "Arun · Plumbing",
                updatedLabel = "Updated 18 min ago",
            ),
            categories = listOf(
                ServiceCategory(ServiceCategoryType.PLUMBING, "Plumbing"),
                ServiceCategory(ServiceCategoryType.ELECTRICAL, "Electrical"),
                ServiceCategory(ServiceCategoryType.APPLIANCE, "Appliance"),
                ServiceCategory(ServiceCategoryType.OTHER, "Other"),
            ),
        )
    }
}

data class MaintenanceRequestSummary(
    val id: String,
    val reference: String = id,
    val title: String,
    val statusLabel: String,
    val statusTone: RequestStatusTone,
    val assignedWorker: String,
    val updatedLabel: String,
)

data class ServiceCategory(
    val type: ServiceCategoryType,
    val label: String,
)

enum class ServiceCategoryType {
    PLUMBING,
    ELECTRICAL,
    APPLIANCE,
    OTHER,
}
