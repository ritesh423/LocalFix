package com.localfix.app.data.local

import androidx.room.Entity
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketCommandType

@Entity(
    tableName = "pending_ticket_commands",
    primaryKeys = ["ticketId", "commandType"],
)
data class PendingTicketCommandEntity(
    val ticketId: String,
    val commandType: TicketCommandType,
    val expectedVersion: Int,
    val priority: String?,
    val workerId: String?,
    val deliveryState: RequestDeliveryState,
    val failureMessage: String?,
    val queuedAt: String,
)
