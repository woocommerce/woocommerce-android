package com.woocommerce.android.ui.subscriptions

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsEligibleForSubscriptions @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    companion object {
        private const val SUBSCRIPTIONS_SLUG = "woocommerce-subscriptions"
    }

    suspend operator fun invoke(): Boolean {
        if (!FeatureFlag.PRODUCT_SUBSCRIPTIONS.isEnabled()) {
            return false
        }

        return wooCommerceStore.getSitePlugins(selectedSite.get())
            .any { it.slug == SUBSCRIPTIONS_SLUG && it.isActive }
    }
}
