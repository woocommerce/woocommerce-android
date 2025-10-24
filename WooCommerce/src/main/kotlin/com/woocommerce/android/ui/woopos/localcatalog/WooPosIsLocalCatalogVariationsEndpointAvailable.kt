package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

class WooPosIsLocalCatalogVariationsEndpointAvailable @Inject constructor(
    private val getWooVersion: GetWooCorePluginCachedVersion,
    private val logger: WooPosLogWrapper
) {
    operator fun invoke(): Boolean {
        val currentWooCoreVersion = getWooVersion() ?: return false.also {
            logger.d("Unknown WooCommerce version - assuming variations endpoint not available.")
        }

        return currentWooCoreVersion.semverCompareTo(WC_VARIATIONS_ENDPOINT_AVAILABLE) >= 0
    }

    companion object {
        private const val WC_VARIATIONS_ENDPOINT_AVAILABLE = "10.3.0"
    }
}
