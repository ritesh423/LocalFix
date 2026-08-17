package com.localfix.app.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResidentTicketDatabaseTest {
    @Test
    fun submittedTicketSurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "resident-ticket-test.db"
        context.deleteDatabase(databaseName)

        val firstDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        firstDatabase.residentTicketDao().upsertTicket(ticketEntity())
        firstDatabase.close()

        val reopenedDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        try {
            val restoredTicket = reopenedDatabase.residentTicketDao().observeTickets().first().single()
            assertEquals("Bathroom pipe is leaking", restoredTicket.title)
            assertEquals(TicketStatus.AWAITING_CONFIRMATION, restoredTicket.status)
            assertEquals(listOf("PVC elbow", "Sealant"), restoredTicket.partsUsed)
        } finally {
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun ticketEntity() = ResidentTicketEntity(
        id = "90000000-0000-0000-0000-000000000001",
        propertyId = "10000000-0000-0000-0000-000000000001",
        unitId = "30000000-0000-0000-0000-000000000204",
        residentId = "40000000-0000-0000-0000-000000000001",
        title = "Bathroom pipe is leaking",
        description = "Water is collecting below the washbasin pipe.",
        category = ServiceCategory.PLUMBING,
        status = TicketStatus.AWAITING_CONFIRMATION,
        urgencySuggestion = UrgencySuggestion.SOON,
        accessWindow = AccessWindow.MORNING,
        assignedWorker = "Aman Verma",
        version = 4,
        completionNote = "Replaced the damaged pipe joint.",
        partsUsed = listOf("PVC elbow", "Sealant"),
        hasCompletionPhoto = true,
        residentRating = null,
        residentFeedback = null,
        createdAt = "2026-08-12T10:00:00Z",
        updatedAt = "2026-08-12T12:30:00Z",
    )
}
