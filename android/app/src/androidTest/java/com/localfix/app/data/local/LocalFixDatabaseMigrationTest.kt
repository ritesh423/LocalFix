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
}
