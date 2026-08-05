package com.localfix.app.ui.profile

data class ResidentProfileUiState(
    val name: String,
    val statusLabel: String,
    val propertyName: String,
    val unitLabel: String,
    val phone: String,
    val email: String,
) {
    companion object {
        val sample = ResidentProfileUiState(
            name = "Ritesh",
            statusLabel = "Resident · Active",
            propertyName = "Lakeview Residency",
            unitLabel = "Apartment A-204",
            phone = "+91 98765 43210",
            email = "ritesh@example.com",
        )
    }
}
