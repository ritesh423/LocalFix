package com.localfix.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalFixDatabaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LocalFixDatabase::class.java,
    )

    @Test
    fun migrationFromOneToTwoKeepsDraftAndAddsOptionalPhoto() {
        val databaseName = "localfix-migration-test.db"
        migrationHelper.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO request_drafts
                    (id, category, title, description, urgencySuggestion, accessWindow)
                VALUES
                    (1, 'PLUMBING', 'Leaking tap', 'Water is dripping below the sink.',
                     'SOON', 'MORNING')
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            MIGRATION_1_2,
        )

        migratedDatabase.query(
            "SELECT title, photoUri FROM request_drafts WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Leaking tap", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
    }

    @Test
    fun migrationFromTwoToThreeKeepsDraftAndAddsClientRequestId() {
        val databaseName = "localfix-migration-two-three-test.db"
        migrationHelper.createDatabase(databaseName, 2).apply {
            execSQL(
                """
                INSERT INTO request_drafts
                    (id, category, title, description, urgencySuggestion, accessWindow, photoUri)
                VALUES
                    (1, 'ELECTRICAL', 'Switch is sparking',
                     'The bedroom switch sparks whenever it is pressed.',
                     'URGENT', 'EVENING', NULL)
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            3,
            true,
            MIGRATION_2_3,
        )

        migratedDatabase.query(
            "SELECT title, clientRequestId FROM request_drafts WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Switch is sparking", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
    }

    @Test
    fun migrationFromThreeToFourKeepsDraftAndAddsResidentTicketCache() {
        val databaseName = "localfix-migration-three-four-test.db"
        migrationHelper.createDatabase(databaseName, 3).apply {
            execSQL(
                """
                INSERT INTO request_drafts
                    (id, clientRequestId, category, title, description,
                     urgencySuggestion, accessWindow, photoUri)
                VALUES
                    (1, '50000000-0000-0000-0000-000000000004', 'PLUMBING',
                     'Leaking tap', 'Water is dripping below the sink.',
                     'SOON', 'MORNING', NULL)
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            MIGRATION_3_4,
        )

        migratedDatabase.query(
            "SELECT title FROM request_drafts WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Leaking tap", cursor.getString(0))
        }
        migratedDatabase.query(
            "SELECT COUNT(*) FROM resident_tickets",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrationFromFourToFiveKeepsTicketsAndAddsPendingQueue() {
        val databaseName = "localfix-migration-four-five-test.db"
        migrationHelper.createDatabase(databaseName, 4).apply {
            execSQL(
                """
                INSERT INTO resident_tickets
                    (id, propertyId, unitId, residentId, title, description,
                     category, status, urgencySuggestion, accessWindow,
                     assignedWorker, version, completionNote, partsUsed,
                     hasCompletionPhoto, residentRating, residentFeedback,
                     createdAt, updatedAt)
                VALUES
                    ('90000000-0000-0000-0000-000000000001', 'property', 'unit',
                     'resident', 'Leaking tap', 'Water is dripping below the sink.',
                     'PLUMBING', 'OPEN', 'SOON', 'MORNING', NULL, 1, NULL, '[]',
                     0, NULL, NULL, '2026-08-17T10:00:00Z', '2026-08-17T10:00:00Z')
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            5,
            true,
            MIGRATION_4_5,
        )

        migratedDatabase.query("SELECT title FROM resident_tickets").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Leaking tap", cursor.getString(0))
        }
        migratedDatabase.query("SELECT COUNT(*) FROM pending_resident_requests").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrationFromFiveToSixKeepsRequestQueueAndAddsReviewQueue() {
        val databaseName = "localfix-migration-five-six-test.db"
        migrationHelper.createDatabase(databaseName, 5).apply {
            execSQL(
                """
                INSERT INTO pending_resident_requests
                    (clientRequestId, title, description, category, urgencySuggestion,
                     accessWindow, photoUri, deliveryState, failureMessage, queuedAt)
                VALUES
                    ('50000000-0000-0000-0000-000000000004', 'Leaking tap',
                     'Water is dripping below the sink.', 'PLUMBING', 'SOON',
                     'MORNING', NULL, 'PENDING', NULL, '2026-08-18T10:00:00Z')
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            MIGRATION_5_6,
        )

        migratedDatabase.query("SELECT title FROM pending_resident_requests").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Leaking tap", cursor.getString(0))
        }
        migratedDatabase.query("SELECT COUNT(*) FROM pending_resident_reviews").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrationFromSixToSevenKeepsReviewQueueAndAddsSharedCommandQueue() {
        val databaseName = "localfix-migration-six-seven-test.db"
        migrationHelper.createDatabase(databaseName, 6).apply {
            execSQL(
                """
                INSERT INTO pending_resident_reviews
                    (ticketId, expectedVersion, decision, rating, feedback,
                     deliveryState, failureMessage, queuedAt)
                VALUES
                    ('90000000-0000-0000-0000-000000000001', 4, 'CONFIRM', 5,
                     'The repair works properly now.', 'PENDING', NULL,
                     '2026-08-18T10:00:00Z')
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            7,
            true,
            MIGRATION_6_7,
        )

        migratedDatabase.query("SELECT rating FROM pending_resident_reviews").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
        }
        migratedDatabase.query("SELECT COUNT(*) FROM pending_ticket_commands").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrationFromSevenToEightKeepsCommandsAndAddsCompletionEvidence() {
        val databaseName = "localfix-migration-seven-eight-test.db"
        migrationHelper.createDatabase(databaseName, 7).apply {
            execSQL(
                """
                INSERT INTO pending_ticket_commands
                    (ticketId, commandType, expectedVersion, priority, workerId,
                     deliveryState, failureMessage, queuedAt)
                VALUES
                    ('90000000-0000-0000-0000-000000000001', 'START', 2, NULL, NULL,
                     'PENDING', NULL, '2026-08-20T10:00:00Z')
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            databaseName,
            8,
            true,
            MIGRATION_7_8,
        )

        migratedDatabase.query(
            "SELECT commandType, completionNote, partsUsed, photoUri " +
                "FROM pending_ticket_commands",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("START", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }
}
