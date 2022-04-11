package com.woocommerce.android.ui.inbox

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxNote.Status
import com.woocommerce.android.ui.inbox.domain.InboxNoteAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class InboxRepository @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchInboxNotes() {
        inboxStore.fetchInboxNotes(selectedSite.get())
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

    private fun InboxNoteWithActions.toInboxNote() =
        InboxNote(
            id = inboxNote.remoteId,
            title = inboxNote.title,
            description = inboxNote.content,
            dateCreated = inboxNote.dateCreated,
            status = inboxNote.status.toInboxNoteStatus(),
            actions = noteActions.map { it.toInboxAction() },
        )

    private fun InboxNoteActionEntity.toInboxAction() =
        InboxNoteAction(
            id = remoteId,
            label = label,
            isPrimary = primary,
            url = url,
        )

    private fun InboxNoteEntity.LocalInboxNoteStatus.toInboxNoteStatus() =
        when (this) {
            InboxNoteEntity.LocalInboxNoteStatus.Unactioned -> Status.UNACTIONED
            InboxNoteEntity.LocalInboxNoteStatus.Actioned -> Status.ACTIONED
            InboxNoteEntity.LocalInboxNoteStatus.Snoozed -> Status.SNOOZED
            InboxNoteEntity.LocalInboxNoteStatus.Unknown -> Status.UNKNOWN
        }
}
