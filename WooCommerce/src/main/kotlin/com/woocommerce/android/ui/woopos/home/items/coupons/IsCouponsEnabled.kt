package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsCouponsEnabled @Inject constructor(
    val wooCommerceStore: WooCommerceStore,
    val selectedSite: SelectedSite
) {
    operator fun invoke(): Boolean {
        return wooCommerceStore.getSiteSettings(selectedSite.get())?.couponsEnabled ?: false
    }
}
