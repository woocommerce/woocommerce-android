package com.woocommerce.android.ui.inbox.domain

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class MarkNoteAsActioned @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(noteId: Long, noteActionId: Long): MarkNoteActionedResult {
        val result = inboxStore.markInboxNoteAsActioned(
            selectedSite.get(),
            noteId,
            noteActionId
        )
        return when {
            result.isError -> Fail
            else -> Success
        }
    }

    sealed class MarkNoteActionedResult
    object Fail : MarkNoteActionedResult()
    object Success : MarkNoteActionedResult()
}
