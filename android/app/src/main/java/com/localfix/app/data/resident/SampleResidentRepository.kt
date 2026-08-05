package com.localfix.app.data.resident

import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.ResidentAccount
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SampleResidentRepository : ResidentRepository {
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
                    category = ServiceCategory.PLUMBING,
                    status = TicketStatus.IN_PROGRESS,
                    assignedWorker = "Arun · Plumbing",
                    updatedLabel = "Updated 18 min ago",
                ),
                MaintenanceRequest(
                    id = "LF-1018",
                    title = "Bedroom switch sparking",
                    category = ServiceCategory.ELECTRICAL,
                    status = TicketStatus.AWAITING_CONFIRMATION,
                    assignedWorker = "Maya · Electrical",
                    updatedLabel = "Completed yesterday",
                ),
                MaintenanceRequest(
                    id = "LF-0994",
                    title = "Washing machine vibration",
                    category = ServiceCategory.APPLIANCE,
                    status = TicketStatus.COMPLETED,
                    assignedWorker = "Sameer · Appliance",
                    updatedLabel = "Closed 12 Jul",
                ),
            ),
            serviceCategories = ServiceCategory.entries,
        ),
    )

    override val residentData: StateFlow<ResidentData> = data.asStateFlow()
}
