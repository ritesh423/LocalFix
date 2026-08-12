package com.localfix.app.ui.requests

data class ResidentRequestsUiState(
    val unitLabel: String,
    val selectedFilter: RequestFilter,
    val requests: List<ResidentRequestItem>,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        val sample = ResidentRequestsUiState(
            unitLabel = "Apartment A-204",
            selectedFilter = RequestFilter.ALL,
            requests = listOf(
                ResidentRequestItem(
                    id = "LF-1042",
                    title = "Leaking kitchen tap",
                    category = "Plumbing",
                    statusTone = RequestStatusTone.ACTIVE,
                    statusLabel = "In progress",
                    updatedLabel = "Updated 18 min ago",
                ),
                ResidentRequestItem(
                    id = "LF-1018",
                    title = "Bedroom switch sparking",
                    category = "Electrical",
                    statusTone = RequestStatusTone.ATTENTION,
                    statusLabel = "Confirm repair",
                    updatedLabel = "Completed yesterday",
                ),
                ResidentRequestItem(
                    id = "LF-0994",
                    title = "Washing machine vibration",
                    category = "Appliance",
                    statusTone = RequestStatusTone.COMPLETED,
                    statusLabel = "Completed",
                    updatedLabel = "Closed 12 Jul",
                ),
            ),
        )
    }
}

data class ResidentRequestItem(
    val id: String,
    val reference: String = id,
    val title: String,
    val category: String,
    val statusTone: RequestStatusTone,
    val statusLabel: String,
    val updatedLabel: String,
)

enum class RequestStatusTone {
    ACTIVE,
    ATTENTION,
    COMPLETED,
    NEUTRAL,
}

enum class RequestFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
}
