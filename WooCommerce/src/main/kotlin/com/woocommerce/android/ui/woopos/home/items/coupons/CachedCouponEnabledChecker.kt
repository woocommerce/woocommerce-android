package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class CachedCouponEnabledChecker @Inject constructor(
    val wooCommerceStore: WooCommerceStore,
    val selectedSite: SelectedSite
) {
    private var cachedSiteId: Int? = null
    private var cachedValue: Boolean? = null

    suspend fun isEnabled(): Boolean {
        val site = selectedSite.get()
        if (cachedSiteId == site.id) {
            cachedValue?.let { return it }
        }

        return (wooCommerceStore.getSiteSettingsAsync(site)?.couponsEnabled ?: false)
            .also {
                cachedSiteId = site.id
                cachedValue = it
            }
    }
}
