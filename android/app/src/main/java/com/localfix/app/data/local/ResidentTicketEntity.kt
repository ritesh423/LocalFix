package com.localfix.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion

@Entity(
    tableName = "resident_tickets",
    indices = [Index(value = ["updatedAt"])],
)
data class ResidentTicketEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val unitId: String,
    val residentId: String,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val status: TicketStatus,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val assignedWorker: String?,
    val version: Int,
    val completionNote: String?,
    val partsUsed: List<String>,
    val hasCompletionPhoto: Boolean,
    val residentRating: Int?,
    val residentFeedback: String?,
    val createdAt: String,
    val updatedAt: String,
)
