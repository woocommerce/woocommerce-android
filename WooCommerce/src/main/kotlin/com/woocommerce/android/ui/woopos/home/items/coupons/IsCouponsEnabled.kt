package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsCouponsEnabled @Inject constructor(
    val wooCommerceStore: WooCommerceStore,
    val selectedSite: SelectedSite
) {
    private var cachedValue: Boolean? = null

    suspend operator fun invoke(): Boolean {
        cachedValue?.let { return it }

        val result = wooCommerceStore.getSiteSettingsAsync(selectedSite.get())?.couponsEnabled ?: false
        cachedValue = result
        return result
    }
}
