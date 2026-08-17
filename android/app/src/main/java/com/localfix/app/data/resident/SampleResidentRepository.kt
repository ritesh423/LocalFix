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
                    version = 4,
                    completionNote = "Replaced the damaged switch and tested it safely.",
                    partsUsed = listOf("16A modular switch"),
                    completionPhotoUrl = "https://example.invalid/completion-photo.jpg",
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
    override val requestSyncState = MutableStateFlow<RequestSyncState>(RequestSyncState.Ready)

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

    override suspend fun refreshRequests() = Unit

    override suspend fun reviewRequest(
        ticketId: String,
        expectedVersion: Int,
        decision: ResidentReviewDecision,
        rating: Int?,
        feedback: String?,
    ) {
        val current = requireNotNull(data.value.requests.find { it.id == ticketId })
        require(current.version == expectedVersion)
        require(current.status == TicketStatus.AWAITING_CONFIRMATION)
        val reviewed = current.copy(
            status = if (decision == ResidentReviewDecision.CONFIRM) {
                TicketStatus.COMPLETED
            } else {
                TicketStatus.ASSIGNED
            },
            version = current.version + 1,
            residentRating = if (decision == ResidentReviewDecision.CONFIRM) rating else null,
            residentFeedback = feedback,
            updatedLabel = "Updated just now",
        )
        data.value = data.value.copy(
            requests = data.value.requests.map { if (it.id == ticketId) reviewed else it },
        )
    }
}
