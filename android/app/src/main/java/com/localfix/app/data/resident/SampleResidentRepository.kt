package com.localfix.app.data.resident

import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ResidentAccount
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SampleResidentRepository : ResidentRepository {
    private var nextRequestNumber = 1043
    private val data = MutableStateFlow(
        ResidentData(
            account = ResidentAccount(
                name = "Ritesh",
                propertyName = "Lakeview Residency",
                unitLabel = "Apartment A-204",
                phone = "+91 98765 43210",
                email = "ritesh@example.com",
            ),
            requests = listOf(
                MaintenanceRequest(
                    id = "LF-1042",
                    title = "Leaking kitchen tap",
                    description = "The tap keeps dripping even when fully closed.",
                    category = ServiceCategory.PLUMBING,
                    status = TicketStatus.IN_PROGRESS,
                    urgencySuggestion = UrgencySuggestion.SOON,
                    accessWindow = AccessWindow.MORNING,
                    assignedWorker = "Arun · Plumbing",
                    updatedLabel = "Updated 18 min ago",
                ),
                MaintenanceRequest(
                    id = "LF-1018",
                    title = "Bedroom switch sparking",
                    description = "A small spark appears when the bedroom light is switched on.",
                    category = ServiceCategory.ELECTRICAL,
                    status = TicketStatus.AWAITING_CONFIRMATION,
                    urgencySuggestion = UrgencySuggestion.URGENT,
                    accessWindow = AccessWindow.EVENING,
                    assignedWorker = "Maya · Electrical",
                    updatedLabel = "Completed yesterday",
                ),
                MaintenanceRequest(
                    id = "LF-0994",
                    title = "Washing machine vibration",
                    description = "The washing machine moves during the spin cycle.",
                    category = ServiceCategory.APPLIANCE,
                    status = TicketStatus.COMPLETED,
                    urgencySuggestion = UrgencySuggestion.ROUTINE,
                    accessWindow = AccessWindow.ANYTIME,
                    assignedWorker = "Sameer · Appliance",
                    updatedLabel = "Closed 12 Jul",
                ),
            ),
            serviceCategories = ServiceCategory.entries,
        ),
    )

    override val residentData: StateFlow<ResidentData> = data.asStateFlow()

    override suspend fun createRequest(request: NewMaintenanceRequest): String {
        val requestId = "LF-${nextRequestNumber++}"
        val newRequest = MaintenanceRequest(
            id = requestId,
            title = request.title,
            description = request.description,
            category = request.category,
            status = TicketStatus.OPEN,
            urgencySuggestion = request.urgencySuggestion,
            accessWindow = request.accessWindow,
            photoUri = request.photoUri,
            assignedWorker = "Awaiting assignment",
            updatedLabel = "Created just now",
        )
        data.value = data.value.copy(
            requests = listOf(newRequest) + data.value.requests,
        )
        return requestId
    }
}
