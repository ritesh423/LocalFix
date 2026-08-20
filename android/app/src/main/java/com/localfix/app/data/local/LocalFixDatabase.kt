package com.localfix.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        RequestDraftEntity::class,
        ResidentTicketEntity::class,
        PendingResidentRequestEntity::class,
        PendingResidentReviewEntity::class,
        PendingTicketCommandEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class LocalFixDatabase : RoomDatabase() {
    abstract fun requestDraftDao(): RequestDraftDao

    abstract fun residentTicketDao(): ResidentTicketDao

    abstract fun pendingResidentRequestDao(): PendingResidentRequestDao

    abstract fun pendingResidentReviewDao(): PendingResidentReviewDao

    abstract fun pendingTicketCommandDao(): PendingTicketCommandDao

    abstract fun residentRequestSyncDao(): ResidentRequestSyncDao
}
