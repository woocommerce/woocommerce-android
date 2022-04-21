package com.woocommerce.android.ui.inbox.domain

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.inbox.domain.InboxNote.NoteType
import com.woocommerce.android.ui.inbox.domain.InboxNote.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class InboxRepository @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchInboxNotes(): Result<Unit> {
        val result = inboxStore.fetchInboxNotes(selectedSite.get())
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    fun observeInboxNotes(): Flow<List<InboxNote>> =
        inboxStore.observeInboxNotes(selectedSite.get().siteId)
            .map { inboxNotesWithActions ->
                inboxNotesWithActions.map { it.toInboxNote() }
            }

    suspend fun markInboxNoteAsActioned(noteId: Long, noteActionId: Long): Result<Unit> {
        val result = inboxStore.markInboxNoteAsActioned(
            selectedSite.get(),
            noteId,
            noteActionId
        )
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun dismissNote(noteId: Long): Result<Unit> {
        val result = inboxStore.deleteNote(selectedSite.get(), noteId)
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun dismissAllNotesForCurrentSite(): Result<Unit> {
        val result = inboxStore.deleteNotesForSite(selectedSite.get())
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    private fun InboxNoteWithActions.toInboxNote() =
        InboxNote(
            id = inboxNote.remoteId,
            title = inboxNote.title,
            description = inboxNote.content,
            dateCreated = inboxNote.dateCreated,
            status = inboxNote.status.toInboxNoteStatus(),
            actions = noteActions.map { it.toInboxAction() },
            type = inboxNoteTypeFromString(inboxNote.type)
        )

    private fun inboxNoteTypeFromString(typeName: String?): NoteType =
        when (typeName) {
            null -> NoteType.INFO
            else -> runCatching {
                enumValueOf(typeName.uppercase()) as NoteType
            }.getOrDefault(NoteType.INFO)
        }

    private fun InboxNoteActionEntity.toInboxAction() =
        InboxNoteAction(
            id = remoteId,
            label = label,
            isPrimary = primary,
            url = url,
        )

    private fun LocalInboxNoteStatus.toInboxNoteStatus() =
        when (this) {
            LocalInboxNoteStatus.Unactioned -> Status.UNACTIONED
            LocalInboxNoteStatus.Actioned -> Status.ACTIONED
            LocalInboxNoteStatus.Snoozed -> Status.SNOOZED
            LocalInboxNoteStatus.Unknown -> Status.UNKNOWN
        }
}
