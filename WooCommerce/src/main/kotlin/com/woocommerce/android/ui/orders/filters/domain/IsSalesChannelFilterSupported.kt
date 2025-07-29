package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

class IsSalesChannelFilterSupported @Inject constructor(
    private val getWooVersion: GetWooCorePluginCachedVersion,
) {
    operator fun invoke(): Boolean {
        val currentWooCoreVersion = getWooVersion() ?: return false
        return currentWooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_SALES_CHANNEL_FILTER) >= 0
    }

    private companion object {
        private const val WC_VERSION_SUPPORTS_SALES_CHANNEL_FILTER = "9.9.0"
    }
}
