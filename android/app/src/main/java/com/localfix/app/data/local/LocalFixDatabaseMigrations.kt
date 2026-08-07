package com.localfix.app.data.local

import androidx.room.migration.Migration

val MIGRATION_1_2 = Migration(1, 2) { database ->
    database.execSQL("ALTER TABLE request_drafts ADD COLUMN photoUri TEXT")
}
