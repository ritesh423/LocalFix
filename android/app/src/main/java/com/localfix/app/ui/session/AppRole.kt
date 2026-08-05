package com.localfix.app.ui.session

enum class AppRole(
    val label: String,
    val description: String,
) {
    RESIDENT(
        label = "Resident",
        description = "Report issues and follow repairs for your apartment.",
    ),
    MANAGER(
        label = "Property manager",
        description = "Review requests, assign workers, and manage exceptions.",
    ),
    WORKER(
        label = "Maintenance worker",
        description = "See assigned jobs, update work, and submit proof.",
    ),
}
