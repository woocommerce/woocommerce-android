package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class GetWooCorePluginCachedVersion @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    operator fun invoke(): String? =
        selectedSite.getOrNull()?.let { selectedSite ->
            wooCommerceStore.getSitePlugin(
                selectedSite,
                WooCommerceStore.WooPlugin.WOO_CORE
            )?.version
        }
}
