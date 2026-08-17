package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentTicketDao {
    @Query("SELECT * FROM resident_tickets ORDER BY updatedAt DESC")
    fun observeTickets(): Flow<List<ResidentTicketEntity>>

    @Query("SELECT COUNT(*) FROM resident_tickets")
    suspend fun countTickets(): Int

    @Upsert
    suspend fun upsertTicket(ticket: ResidentTicketEntity)

    @Upsert
    suspend fun upsertTickets(tickets: List<ResidentTicketEntity>)

    @Query("DELETE FROM resident_tickets")
    suspend fun deleteAllTickets()

    @Transaction
    suspend fun replaceAllTickets(tickets: List<ResidentTicketEntity>) {
        deleteAllTickets()
        upsertTickets(tickets)
    }
}
