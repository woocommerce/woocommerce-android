package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import javax.inject.Inject

class WooPosIsLocalCatalogSupported @Inject constructor(
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val isFileBasedSyncSupported: WooPosIsFileBasedSyncSupported,
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

        if (!isFileBasedSyncSupported()) {
            return false.also {
                wooPosLogWrapper.d(
                    "Local Catalog not supported: WooCommerce version does not support" +
                        " file-based sync (requires ${WooPosIsFileBasedSyncSupported.MIN_WC_VERSION})."
                )
            }
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

        return true.also { wooPosLogWrapper.d("Local Catalog supported: POS is using the local catalog.") }
    }

    /**
     * The same verdict from state that has already been recorded: [tabVisible] and [launchable] are the prefs
     * [invoke] writes as it evaluates, and the Woo version comes from the cache rather than a fetch. For callers
     * that must not mutate or block on what they report — the Mobile Status Report is one. [invoke] logs its real
     * answer, so the log on the same ticket settles any disagreement between the two.
     */
    fun asOfLastEvaluation(tabVisible: Boolean, launchable: Boolean): Boolean =
        wooPosLocalCatalogM1Enabled() && isFileBasedSyncSupported.fromCachedVersion() && tabVisible && launchable
}
