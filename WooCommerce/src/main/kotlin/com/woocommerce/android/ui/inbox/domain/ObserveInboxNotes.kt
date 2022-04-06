package com.woocommerce.android.ui.inbox.domain

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
            else -> enumValueOf(typeName.uppercase())
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
