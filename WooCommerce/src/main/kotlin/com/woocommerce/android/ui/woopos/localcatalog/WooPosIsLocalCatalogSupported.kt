package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import javax.inject.Inject

/**
 * Checks if local catalog is supported and enabled for a given site.
 */
class WooPosIsLocalCatalogSupported @Inject constructor(
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val prefsRepo: WooPosPreferencesRepository,
    private val isVariationsEndpointAvailable: WooPosIsLocalCatalogVariationsEndpointAvailable,
    private val posTabShouldBeVisible: WooPosTabShouldBeVisible,
    private val posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(localSiteId: LocalOrRemoteId.LocalId): Boolean {
        if (!wooPosLocalCatalogM1Enabled()) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: Feature flag disabled.")
            }
        }

        if (!isVariationsEndpointAvailable()) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: Variations endpoint not available.")
            }
        }

        if (!prefsRepo.isPeriodicSyncEnabledForSite(localSiteId)) {
            return false.also {
                wooPosLogWrapper.d("Local Catalog not supported: Periodic sync disabled.")
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

        return true
    }
}
