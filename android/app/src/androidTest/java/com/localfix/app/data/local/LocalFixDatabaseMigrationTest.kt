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
}
