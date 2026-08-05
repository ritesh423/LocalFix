package com.localfix.app.ui.requests

data class ResidentRequestsUiState(
    val requests: List<ResidentRequestItem>,
) {
    companion object {
        val sample = ResidentRequestsUiState(
            requests = listOf(
                ResidentRequestItem(
                    id = "LF-1042",
                    title = "Leaking kitchen tap",
                    category = "Plumbing",
                    status = RequestStatus.IN_PROGRESS,
                    statusLabel = "In progress",
                    updatedLabel = "Updated 18 min ago",
                ),
                ResidentRequestItem(
                    id = "LF-1018",
                    title = "Bedroom switch sparking",
                    category = "Electrical",
                    status = RequestStatus.AWAITING_CONFIRMATION,
                    statusLabel = "Confirm repair",
                    updatedLabel = "Completed yesterday",
                ),
                ResidentRequestItem(
                    id = "LF-0994",
                    title = "Washing machine vibration",
                    category = "Appliance",
                    status = RequestStatus.COMPLETED,
                    statusLabel = "Completed",
                    updatedLabel = "Closed 12 Jul",
                ),
            ),
        )
    }
}

data class ResidentRequestItem(
    val id: String,
    val title: String,
    val category: String,
    val status: RequestStatus,
    val statusLabel: String,
    val updatedLabel: String,
)

enum class RequestStatus {
    IN_PROGRESS,
    AWAITING_CONFIRMATION,
    COMPLETED,
}

enum class RequestFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
}
