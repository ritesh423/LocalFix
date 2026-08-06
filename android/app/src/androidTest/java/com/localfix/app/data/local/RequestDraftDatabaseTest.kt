package com.localfix.app.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestDraftDatabaseTest {
    @Test
    fun draftSurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "request-draft-test.db"
        context.deleteDatabase(databaseName)

        val firstDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        firstDatabase.requestDraftDao().saveDraft(
            RequestDraftEntity(
                category = ServiceCategory.ELECTRICAL,
                title = "Hallway light flickers",
                description = "The hallway light flickers for several seconds after switching on.",
                urgencySuggestion = UrgencySuggestion.SOON,
                accessWindow = AccessWindow.EVENING,
            ),
        )
        firstDatabase.close()

        val reopenedDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        try {
            val restoredDraft = reopenedDatabase.requestDraftDao().observeDraft().first()
            assertEquals("Hallway light flickers", restoredDraft?.title)
            assertEquals(ServiceCategory.ELECTRICAL, restoredDraft?.category)

            reopenedDatabase.requestDraftDao().clearDraft()
            assertEquals(null, reopenedDatabase.requestDraftDao().observeDraft().first())
        } finally {
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }
}
