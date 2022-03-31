package com.woocommerce.android.ui.inbox.domain

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class ObserveInboxNotes @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<List<InboxNote>> =
        inboxStore.observeInboxNotes(selectedSite.get().siteId)
            .map { inboxNotesWithActions ->
                inboxNotesWithActions.map { it.toInboxNote() }
            }

    private fun InboxNoteWithActions.toInboxNote() =
        InboxNote(
            id = inboxNote.id,
            title = inboxNote.title,
            description = inboxNote.content,
            dateCreated = inboxNote.dateCreated,
            status = inboxNote.status.toInboxNoteStatus(),
            actions = noteActions.map { it.toInboxAction() },
        )

    private fun InboxNoteActionEntity.toInboxAction() =
        InboxNoteAction(
            id = id,
            label = label,
            isPrimary = primary,
            url = url,
        )

    private fun LocalInboxNoteStatus.toInboxNoteStatus() =
        when (this) {
            LocalInboxNoteStatus.Unactioned -> InboxNote.Status.Unactioned
            LocalInboxNoteStatus.Actioned -> InboxNote.Status.Actioned
            LocalInboxNoteStatus.Snoozed -> InboxNote.Status.Snoozed
            LocalInboxNoteStatus.Unknown -> InboxNote.Status.Unknown
        }
}
