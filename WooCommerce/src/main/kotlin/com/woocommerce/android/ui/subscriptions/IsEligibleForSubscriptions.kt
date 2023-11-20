package com.woocommerce.android.ui.subscriptions

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsEligibleForSubscriptions @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    companion object {
        private const val SUBSCRIPTIONS_SLUG = "woocommerce-subscriptions"
    }

    suspend operator fun invoke(): Boolean =
        wooCommerceStore.getSitePlugins(selectedSite.get())
            .any { it.slug == SUBSCRIPTIONS_SLUG && it.isActive }
}
