package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface ResidentRequestSyncDao {
    @Upsert
    suspend fun upsertTicket(ticket: ResidentTicketEntity)

    @Upsert
    suspend fun upsertTickets(tickets: List<ResidentTicketEntity>)

    @Query("DELETE FROM resident_tickets")
    suspend fun deleteAllTickets()

    @Query("DELETE FROM pending_resident_requests WHERE clientRequestId = :clientRequestId")
    suspend fun deletePendingRequest(clientRequestId: String)

    @Query("DELETE FROM pending_resident_requests WHERE clientRequestId IN (:clientRequestIds)")
    suspend fun deleteAcknowledgedRequests(clientRequestIds: List<String>)

    @Transaction
    suspend fun completePendingRequest(
        clientRequestId: String,
        ticket: ResidentTicketEntity,
    ) {
        upsertTicket(ticket)
        deletePendingRequest(clientRequestId)
    }

    @Transaction
    suspend fun replaceServerSnapshot(
        tickets: List<ResidentTicketEntity>,
        acknowledgedClientRequestIds: List<String>,
    ) {
        deleteAllTickets()
        upsertTickets(tickets)
        if (acknowledgedClientRequestIds.isNotEmpty()) {
            deleteAcknowledgedRequests(acknowledgedClientRequestIds)
        }
    }
}
