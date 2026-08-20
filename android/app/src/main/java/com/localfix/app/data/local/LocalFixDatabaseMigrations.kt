package com.localfix.app.data.local

import androidx.room.migration.Migration

val MIGRATION_1_2 = Migration(1, 2) { database ->
    database.execSQL("ALTER TABLE request_drafts ADD COLUMN photoUri TEXT")
}

val MIGRATION_2_3 = Migration(2, 3) { database ->
    database.execSQL(
        "ALTER TABLE request_drafts ADD COLUMN clientRequestId TEXT NOT NULL DEFAULT ''",
    )
}

val MIGRATION_3_4 = Migration(3, 4) { database ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS resident_tickets (
            id TEXT NOT NULL PRIMARY KEY,
            propertyId TEXT NOT NULL,
            unitId TEXT NOT NULL,
            residentId TEXT NOT NULL,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            category TEXT NOT NULL,
            status TEXT NOT NULL,
            urgencySuggestion TEXT NOT NULL,
            accessWindow TEXT NOT NULL,
            assignedWorker TEXT,
            version INTEGER NOT NULL,
            completionNote TEXT,
            partsUsed TEXT NOT NULL,
            hasCompletionPhoto INTEGER NOT NULL,
            residentRating INTEGER,
            residentFeedback TEXT,
            createdAt TEXT NOT NULL,
            updatedAt TEXT NOT NULL
        )
        """.trimIndent(),
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_resident_tickets_updatedAt " +
            "ON resident_tickets(updatedAt)",
    )
}

val MIGRATION_4_5 = Migration(4, 5) { database ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS pending_resident_requests (
            clientRequestId TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            category TEXT NOT NULL,
            urgencySuggestion TEXT NOT NULL,
            accessWindow TEXT NOT NULL,
            photoUri TEXT,
            deliveryState TEXT NOT NULL,
            failureMessage TEXT,
            queuedAt TEXT NOT NULL
        )
        """.trimIndent(),
    )
}

val MIGRATION_5_6 = Migration(5, 6) { database ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS pending_resident_reviews (
            ticketId TEXT NOT NULL PRIMARY KEY,
            expectedVersion INTEGER NOT NULL,
            decision TEXT NOT NULL,
            rating INTEGER,
            feedback TEXT,
            deliveryState TEXT NOT NULL,
            failureMessage TEXT,
            queuedAt TEXT NOT NULL
        )
        """.trimIndent(),
    )
}

val MIGRATION_6_7 = Migration(6, 7) { database ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS pending_ticket_commands (
            ticketId TEXT NOT NULL,
            commandType TEXT NOT NULL,
            expectedVersion INTEGER NOT NULL,
            priority TEXT,
            workerId TEXT,
            deliveryState TEXT NOT NULL,
            failureMessage TEXT,
            queuedAt TEXT NOT NULL,
            PRIMARY KEY(ticketId, commandType)
        )
        """.trimIndent(),
    )
}
