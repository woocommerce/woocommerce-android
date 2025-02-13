package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions

@Dao
abstract class InboxNotesDao {
    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE localSiteId = :localSiteId ORDER BY dateCreated DESC, remoteId DESC")
    abstract fun observeInboxNotes(localSiteId: LocalId): Flow<List<InboxNoteWithActions>>

    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE localSiteId = :localSiteId")
    abstract fun getInboxNotesForSite(localSiteId: LocalId): List<InboxNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNote(entity: InboxNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNoteAction(entity: InboxNoteActionEntity)

    @Transaction
    @Query("DELETE FROM InboxNotes WHERE localSiteId = :localSiteId")
    abstract fun deleteInboxNotesForSite(localSiteId: LocalId)

    @Query("DELETE FROM InboxNotes WHERE remoteId = :remoteNoteId AND localSiteId = :localSiteId")
    abstract suspend fun deleteInboxNote(remoteNoteId: Long, localSiteId: LocalId)

    @Transaction
    open suspend fun deleteAllAndInsertInboxNotes(
        localSiteId: LocalId,
        vararg notes: InboxNoteWithActions
    ) {
        deleteInboxNotesForSite(localSiteId)
        notes.forEach { noteWithActions ->
            val localNoteId = insertOrUpdateInboxNote(noteWithActions.inboxNote)
            noteWithActions.noteActions.forEach {
                insertOrUpdateInboxNoteAction(it.copy(inboxNoteLocalId = localNoteId))
            }
        }
    }

    @Transaction
    open suspend fun updateNote(localSiteId: LocalId, noteWithActions: InboxNoteWithActions) {
        deleteInboxNote(noteWithActions.inboxNote.remoteId, localSiteId)
        val localNoteId = insertOrUpdateInboxNote(noteWithActions.inboxNote)
        noteWithActions.noteActions.forEach {
            insertOrUpdateInboxNoteAction(it.copy(inboxNoteLocalId = localNoteId))
        }
    }
}
