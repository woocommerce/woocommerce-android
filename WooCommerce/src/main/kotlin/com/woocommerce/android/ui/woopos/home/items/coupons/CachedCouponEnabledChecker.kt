package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.extensions.isCIABSite
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class CachedCouponEnabledChecker @Inject constructor(
    val wooCommerceStore: WooCommerceStore,
    val selectedSite: SelectedSite
) {
    private var cachedValue: Boolean? = null

    suspend fun isEnabled(): Boolean {
        cachedValue?.let { return it }

        val enabled = if (selectedSite.get().isCIABSite()) {
            true
        } else {
            wooCommerceStore.getSiteSettingsAsync(selectedSite.get())?.couponsEnabled ?: false
        }
        cachedValue = enabled
        return enabled
    }
}
