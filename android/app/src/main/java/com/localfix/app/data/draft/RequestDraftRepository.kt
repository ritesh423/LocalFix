package com.localfix.app.data.draft

import com.localfix.app.data.model.SavedRequestDraft
import kotlinx.coroutines.flow.Flow

interface RequestDraftRepository {
    fun observeDraft(): Flow<SavedRequestDraft?>

    suspend fun saveDraft(draft: SavedRequestDraft)

    suspend fun clearDraft()
}
