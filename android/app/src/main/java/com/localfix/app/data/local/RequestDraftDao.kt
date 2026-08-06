package com.localfix.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDraftDao {
    @Query("SELECT * FROM request_drafts WHERE id = :draftId")
    fun observeDraft(draftId: Int = CURRENT_DRAFT_ID): Flow<RequestDraftEntity?>

    @Upsert
    suspend fun saveDraft(draft: RequestDraftEntity)

    @Query("DELETE FROM request_drafts WHERE id = :draftId")
    suspend fun clearDraft(draftId: Int = CURRENT_DRAFT_ID)
}
