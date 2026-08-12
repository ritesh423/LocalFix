package com.localfix.app.data.draft

import com.localfix.app.data.local.RequestDraftDao
import com.localfix.app.data.local.RequestDraftEntity
import com.localfix.app.data.model.SavedRequestDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomRequestDraftRepository(
    private val dao: RequestDraftDao,
) : RequestDraftRepository {
    override fun observeDraft(): Flow<SavedRequestDraft?> = dao.observeDraft().map { entity ->
        entity?.toModel()
    }

    override suspend fun saveDraft(draft: SavedRequestDraft) {
        dao.saveDraft(draft.toEntity())
    }

    override suspend fun clearDraft() {
        dao.clearDraft()
    }
}

private fun RequestDraftEntity.toModel(): SavedRequestDraft = SavedRequestDraft(
    clientRequestId = clientRequestId.ifBlank { UUID.randomUUID().toString() },
    category = category,
    title = title,
    description = description,
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
)

private fun SavedRequestDraft.toEntity(): RequestDraftEntity = RequestDraftEntity(
    clientRequestId = clientRequestId,
    category = category,
    title = title,
    description = description,
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
)
