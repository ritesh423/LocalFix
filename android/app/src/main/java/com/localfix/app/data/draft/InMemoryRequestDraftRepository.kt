package com.localfix.app.data.draft

import com.localfix.app.data.model.SavedRequestDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryRequestDraftRepository(
    initialDraft: SavedRequestDraft? = null,
) : RequestDraftRepository {
    private val draft = MutableStateFlow(initialDraft)

    override fun observeDraft(): Flow<SavedRequestDraft?> = draft

    override suspend fun saveDraft(draft: SavedRequestDraft) {
        this.draft.value = draft
    }

    override suspend fun clearDraft() {
        draft.value = null
    }
}
