package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketCommandType
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTicketCommandDao {
    @Query("SELECT * FROM pending_ticket_commands ORDER BY queuedAt DESC")
    fun observeCommands(): Flow<List<PendingTicketCommandEntity>>

    @Query(
        "SELECT * FROM pending_ticket_commands " +
            "WHERE ticketId = :ticketId AND commandType = :commandType",
    )
    suspend fun getCommand(
        ticketId: String,
        commandType: TicketCommandType,
    ): PendingTicketCommandEntity?

    @Query("SELECT * FROM pending_ticket_commands WHERE deliveryState = 'PENDING'")
    suspend fun getRetryableCommands(): List<PendingTicketCommandEntity>

    @Upsert
    suspend fun upsertCommand(command: PendingTicketCommandEntity)

    @Query(
        "UPDATE pending_ticket_commands " +
            "SET deliveryState = :deliveryState, failureMessage = :message " +
            "WHERE ticketId = :ticketId AND commandType = :commandType",
    )
    suspend fun updateDeliveryState(
        ticketId: String,
        commandType: TicketCommandType,
        deliveryState: RequestDeliveryState,
        message: String?,
    )

    @Query(
        "DELETE FROM pending_ticket_commands " +
            "WHERE ticketId = :ticketId AND commandType = :commandType",
    )
    suspend fun deleteCommand(ticketId: String, commandType: TicketCommandType)
}
