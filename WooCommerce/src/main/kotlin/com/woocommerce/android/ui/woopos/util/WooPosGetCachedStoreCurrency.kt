package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosGetCachedStoreCurrency @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    private lateinit var currencyCode: String

    suspend operator fun invoke(): String {
        if (!::currencyCode.isInitialized) {
            withContext(Dispatchers.IO) {
                currencyCode = requireNotNull(wooCommerceStore.getSiteSettings(selectedSite.get())) {
                    "Site settings not found"
                }.currencyCode
            }
        }
        return currencyCode
    }
}
