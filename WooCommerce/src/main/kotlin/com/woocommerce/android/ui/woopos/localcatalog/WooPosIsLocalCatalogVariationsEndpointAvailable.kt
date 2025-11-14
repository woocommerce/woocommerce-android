package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosIsLocalCatalogVariationsEndpointAvailable @Inject constructor(
    private val getWooVersion: GetWooCorePluginCachedVersion,
    private val fetchWooVersion: FetchActiveWCPluginVersion,
    private val logger: WooPosLogWrapper,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(): Boolean = withContext(dispatchers.io) {
        val currentWooCoreVersion = getWooVersion() ?: fetchWooVersion() ?: return@withContext false.also {
            logger.d("Unknown WooCommerce version - assuming variations endpoint not available.")
        }

        return@withContext currentWooCoreVersion.semverCompareTo(WC_VARIATIONS_ENDPOINT_AVAILABLE) >= 0
    }

    companion object {
        private const val WC_VARIATIONS_ENDPOINT_AVAILABLE = "10.3.0"
    }
}
