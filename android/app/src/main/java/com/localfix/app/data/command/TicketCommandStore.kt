package com.localfix.app.data.command

import com.localfix.app.data.local.PendingTicketCommandDao
import com.localfix.app.data.local.PendingTicketCommandEntity
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketCommandType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface TicketCommandStore {
    fun observeCommands(): Flow<List<PendingTicketCommandEntity>>

    suspend fun getCommand(
        ticketId: String,
        commandType: TicketCommandType,
    ): PendingTicketCommandEntity?

    suspend fun getRetryableCommands(): List<PendingTicketCommandEntity>

    suspend fun queue(command: PendingTicketCommandEntity)

    suspend fun markFailed(
        ticketId: String,
        commandType: TicketCommandType,
        message: String,
    )

    suspend fun complete(ticketId: String, commandType: TicketCommandType)
}

class RoomTicketCommandStore(
    private val dao: PendingTicketCommandDao,
) : TicketCommandStore {
    override fun observeCommands(): Flow<List<PendingTicketCommandEntity>> =
        dao.observeCommands()

    override suspend fun getCommand(
        ticketId: String,
        commandType: TicketCommandType,
    ): PendingTicketCommandEntity? = dao.getCommand(ticketId, commandType)

    override suspend fun getRetryableCommands(): List<PendingTicketCommandEntity> =
        dao.getRetryableCommands()

    override suspend fun queue(command: PendingTicketCommandEntity) {
        dao.upsertCommand(command)
    }

    override suspend fun markFailed(
        ticketId: String,
        commandType: TicketCommandType,
        message: String,
    ) {
        dao.updateDeliveryState(
            ticketId = ticketId,
            commandType = commandType,
            deliveryState = RequestDeliveryState.FAILED,
            message = message,
        )
    }

    override suspend fun complete(ticketId: String, commandType: TicketCommandType) {
        dao.deleteCommand(ticketId, commandType)
    }
}

class InMemoryTicketCommandStore(
    initialCommands: List<PendingTicketCommandEntity> = emptyList(),
) : TicketCommandStore {
    private val commands = MutableStateFlow(initialCommands)

    override fun observeCommands(): Flow<List<PendingTicketCommandEntity>> = commands

    override suspend fun getCommand(
        ticketId: String,
        commandType: TicketCommandType,
    ): PendingTicketCommandEntity? = commands.value.find {
        it.ticketId == ticketId && it.commandType == commandType
    }

    override suspend fun getRetryableCommands(): List<PendingTicketCommandEntity> =
        commands.value.filter { it.deliveryState == RequestDeliveryState.PENDING }

    override suspend fun queue(command: PendingTicketCommandEntity) {
        commands.value = listOf(command) + commands.value.filterNot {
            it.ticketId == command.ticketId && it.commandType == command.commandType
        }
    }

    override suspend fun markFailed(
        ticketId: String,
        commandType: TicketCommandType,
        message: String,
    ) {
        commands.value = commands.value.map {
            if (it.ticketId == ticketId && it.commandType == commandType) {
                it.copy(deliveryState = RequestDeliveryState.FAILED, failureMessage = message)
            } else {
                it
            }
        }
    }

    override suspend fun complete(ticketId: String, commandType: TicketCommandType) {
        commands.value = commands.value.filterNot {
            it.ticketId == ticketId && it.commandType == commandType
        }
    }
}
