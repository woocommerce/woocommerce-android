package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import javax.inject.Inject

/**
 * Checks if local catalog is supported and enabled for a given site.
 */
class WooPosIsLocalCatalogSupported @Inject constructor(
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val prefsRepo: WooPosPreferencesRepository,
    private val isVariationsEndpointAvailable: WooPosIsLocalCatalogVariationsEndpointAvailable,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(siteId: Long): Boolean {
        if (!wooPosLocalCatalogM1Enabled()) {
            return false
        }

        if (!isVariationsEndpointAvailable()) {
            return false
        }

        if (!prefsRepo.isPeriodicSyncEnabledForSite(siteId)) {
            return false
        }

        return true
    }
}
