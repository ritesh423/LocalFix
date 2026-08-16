package com.localfix.app.data.worker

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion

data class WorkerData(
    val workerName: String,
    val propertyName: String,
    val jobs: List<WorkerJob>,
)

data class WorkerJob(
    val id: String,
    val reference: String,
    val unitLabel: String,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val urgencySuggestion: UrgencySuggestion,
    val priorityLabel: String,
    val accessWindow: AccessWindow,
    val status: TicketStatus,
    val version: Int,
    val updatedLabel: String,
)
