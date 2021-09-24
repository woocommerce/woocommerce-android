package com.woocommerce.android.ui.prefs.cardreader.connect

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject

class CardReaderLocationRepository @Inject constructor(
    private val wcPayStore: WCPayStore,
    private val selectedSite: SelectedSite
) {
    suspend fun getDefaultLocationId(): String? {
        val selectedSite = selectedSite.getIfExists() ?: return null
        val result = wcPayStore.getStoreLocationForSite(selectedSite)
        return if (result.isError) {
            // TODO cardreader handle the error
            null
        } else {
            result.locationId!!
        }
    }
}
