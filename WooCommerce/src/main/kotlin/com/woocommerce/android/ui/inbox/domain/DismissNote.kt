package com.woocommerce.android.ui.inbox.domain

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class DismissNote @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(noteId: Long) {
        inboxStore.deleteNote(selectedSite.get(), noteId)
    }
}
