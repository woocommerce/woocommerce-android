package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

class WooPosIsLocalCatalogSupported @Inject constructor(
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val getWooVersion: GetWooCorePluginCachedVersion,
    private val fetchWooVersion: FetchActiveWCPluginVersion,
    private val posTabShouldBeVisible: WooPosTabShouldBeVisible,
    private val posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean {
        if (!wooPosLocalCatalogM1Enabled()) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: Feature flag disabled.")
            }
        }

        if (!isSyncApproachSupported()) {
            return false
        }

        val tabVisibleResult = posTabShouldBeVisible()
        if (tabVisibleResult.isFailure || tabVisibleResult.getOrNull() != true) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: POS Tab not visible.")
            }
        }

        val launchability = posCanBeLaunchedInTab()
        if (launchability !is WooPosLaunchability.Launchable) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: POS not launchable: $launchability.")
            }
        }

        // The Mobile Status Report derives this same verdict from cached prefs, because evaluating it here
        // writes them. Logging the real answer lets support tell the two apart on a ticket.
        return true.also { wooPosLogWrapper.d("Local Catalog supported: POS is using the local catalog.") }
    }

    private suspend fun isSyncApproachSupported(): Boolean {
        if (!isFileBasedSyncSupported()) {
            wooPosLogWrapper.d(
                "Local Catalog not supported: WooCommerce version does not support" +
                    " file-based sync (requires $WC_FILE_BASED_SYNC_MIN_VERSION)."
            )
            return false
        }
        return true
    }

    private suspend fun isFileBasedSyncSupported(): Boolean {
        val wooVersion = getWooVersion() ?: fetchWooVersion() ?: return false
        return wooVersion.semverCompareTo(WC_FILE_BASED_SYNC_MIN_VERSION) >= 0
    }

    companion object {
        private const val WC_FILE_BASED_SYNC_MIN_VERSION = "10.5.0"
    }
}
