package com.localfix.app.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingResidentRequestDatabaseTest {
    @Test
    fun queuedRequestAndPhotoSurviveDatabaseCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "pending-resident-request-test.db"
        context.deleteDatabase(databaseName)

        val firstDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        firstDatabase.pendingResidentRequestDao().upsertRequest(pendingRequest())
        firstDatabase.close()

        val reopenedDatabase = Room.databaseBuilder(
            context,
            LocalFixDatabase::class.java,
            databaseName,
        ).build()
        try {
            val restored = reopenedDatabase.pendingResidentRequestDao()
                .observeRequests()
                .first()
                .single()
            assertEquals("Kitchen tap is leaking", restored.title)
            assertEquals(RequestDeliveryState.PENDING, restored.deliveryState)
            assertEquals("content://localfix/photo/kitchen-tap", restored.photoUri)
        } finally {
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun pendingRequest() = PendingResidentRequestEntity(
        clientRequestId = "50000000-0000-0000-0000-000000000004",
        title = "Kitchen tap is leaking",
        description = "Water continues dripping after the tap is fully closed.",
        category = ServiceCategory.PLUMBING,
        urgencySuggestion = UrgencySuggestion.SOON,
        accessWindow = AccessWindow.MORNING,
        photoUri = "content://localfix/photo/kitchen-tap",
        deliveryState = RequestDeliveryState.PENDING,
        failureMessage = null,
        queuedAt = "2026-08-17T10:00:00Z",
    )
}
