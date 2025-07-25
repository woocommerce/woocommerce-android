package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosGetStoreCountryCode @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend operator fun invoke(): String? = withContext(Dispatchers.IO) {
        val site = selectedSite.getOrNull() ?: return@withContext null
        val siteSettings = wooCommerceStore.getSiteSettings(site)
            ?: wooCommerceStore.fetchSiteGeneralSettings(site).model
        siteSettings?.countryCode?.lowercase()
    }
}
