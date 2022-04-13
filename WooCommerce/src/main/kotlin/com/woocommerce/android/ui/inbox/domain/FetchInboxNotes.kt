package com.woocommerce.android.ui.inbox.domain

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class FetchInboxNotes @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke() {
        inboxStore.fetchInboxNotes(selectedSite.get())
    }
}
