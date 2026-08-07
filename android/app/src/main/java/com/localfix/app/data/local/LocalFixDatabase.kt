package com.localfix.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RequestDraftEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class LocalFixDatabase : RoomDatabase() {
    abstract fun requestDraftDao(): RequestDraftDao
}
