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
