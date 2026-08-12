package com.localfix.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion

@Entity(tableName = "request_drafts")
data class RequestDraftEntity(
    @PrimaryKey val id: Int = CURRENT_DRAFT_ID,
    val clientRequestId: String,
    val category: ServiceCategory?,
    val title: String,
    val description: String,
    val urgencySuggestion: UrgencySuggestion,
    val accessWindow: AccessWindow,
    val photoUri: String? = null,
)

const val CURRENT_DRAFT_ID = 1
