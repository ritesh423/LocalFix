package com.localfix.app.data.manager

import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SampleManagerRepository : ManagerRepository {
    private val mutableData = MutableStateFlow(sampleManagerData())

    override val managerData: StateFlow<ManagerData> = mutableData.asStateFlow()
    override val syncState = MutableStateFlow<ManagerSyncState>(ManagerSyncState.Ready)

    override suspend fun refresh() = Unit

    override suspend fun createResidentInvite(
        unitId: String,
        validDays: Int,
    ): ManagerResidentInvite {
        val unit = requireNotNull(mutableData.value.units.find { it.id == unitId })
        return ManagerResidentInvite(
            inviteCode = "LF-7K9M-4N2P-8QRT",
            unitId = unit.id,
            unitLabel = unit.label,
            expiresAt = "2026-09-07T12:00:00Z",
        )
    }

    override suspend fun createWorkerInvite(
        name: String,
        specialty: ServiceCategory,
        validDays: Int,
    ): ManagerWorkerInvite {
        val worker = ManagerWorker(
            id = "40000000-0000-0000-0000-000000000004",
            name = name,
            specialty = specialty,
        )
        mutableData.update { data -> data.copy(workers = data.workers + worker) }
        return ManagerWorkerInvite(
            inviteCode = "LFW-7K9M-4N2P-8QRT",
            worker = worker,
            expiresAt = "2026-09-08T12:00:00Z",
        )
    }

    override suspend fun assignTicket(
        ticketId: String,
        expectedVersion: Int,
        priority: ManagerPriority,
        workerId: String,
    ): ManagerTicket {
        val worker = requireNotNull(mutableData.value.workers.find { it.id == workerId })
        val current = requireNotNull(mutableData.value.tickets.find { it.id == ticketId })
        require(current.version == expectedVersion)
        val assigned = current.copy(
            priority = priority,
            status = TicketStatus.ASSIGNED,
            version = current.version + 1,
            assignedWorkerId = worker.id,
            assignedWorker = worker.name,
            updatedLabel = "Updated just now",
        )
        mutableData.update { data ->
            data.copy(
                tickets = data.tickets.map { ticket ->
                    if (ticket.id == assigned.id) assigned else ticket
                },
                summary = data.summary.copy(
                    needsAssignment = data.summary.needsAssignment - 1,
                    assigned = data.summary.assigned + 1,
                ),
            )
        }
        return assigned
    }
}

fun sampleManagerData() = ManagerData(
    propertyName = "Lakeview Residency",
    tickets = listOf(
        ManagerTicket(
            id = "90000000-0000-0000-0000-000000000001",
            reference = "LF-90000000",
            unitLabel = "Apartment A-204",
            title = "Bathroom pipe is leaking",
            description = "Water is collecting below the bathroom washbasin pipe.",
            category = ServiceCategory.PLUMBING,
            urgencySuggestion = UrgencySuggestion.SOON,
            priority = null,
            accessWindow = AccessWindow.MORNING,
            status = TicketStatus.OPEN,
            version = 1,
            assignedWorkerId = null,
            assignedWorker = null,
            updatedLabel = "Updated 12 min ago",
        ),
        ManagerTicket(
            id = "90000000-0000-0000-0000-000000000002",
            reference = "LF-90000001",
            unitLabel = "Apartment B-108",
            title = "Bedroom switch is sparking",
            description = "A small spark appears when the bedroom light is switched on.",
            category = ServiceCategory.ELECTRICAL,
            urgencySuggestion = UrgencySuggestion.URGENT,
            priority = ManagerPriority.URGENT,
            accessWindow = AccessWindow.EVENING,
            status = TicketStatus.ASSIGNED,
            version = 2,
            assignedWorkerId = "40000000-0000-0000-0000-000000000002",
            assignedWorker = "Maya Singh",
            updatedLabel = "Updated 1 hr ago",
        ),
    ),
    workers = listOf(
        ManagerWorker(
            id = "40000000-0000-0000-0000-000000000001",
            name = "Arun Kumar",
            specialty = ServiceCategory.PLUMBING,
        ),
        ManagerWorker(
            id = "40000000-0000-0000-0000-000000000002",
            name = "Maya Singh",
            specialty = ServiceCategory.ELECTRICAL,
        ),
        ManagerWorker(
            id = "40000000-0000-0000-0000-000000000003",
            name = "Sameer Khan",
            specialty = ServiceCategory.APPLIANCE,
        ),
    ),
    units = listOf(
        ManagerPropertyUnit(
            id = "30000000-0000-0000-0000-000000000204",
            label = "Apartment A-204",
        ),
        ManagerPropertyUnit(
            id = "30000000-0000-0000-0000-000000000108",
            label = "Apartment B-108",
        ),
    ),
    summary = ManagerSummary(
        totalRequests = 2,
        activeRequests = 2,
        needsAssignment = 1,
        assigned = 1,
        inProgress = 0,
        blocked = 0,
        awaitingConfirmation = 0,
        completed = 0,
    ),
)
