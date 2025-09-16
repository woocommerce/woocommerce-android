package com.woocommerce.android.ui.blaze

import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.Jetpack
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper
) {
    companion object {
        private const val BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG = "blaze-ads"
    }

    operator fun invoke(): Boolean {
        val site = selectedSite.getOrNull() ?: return false
        return site.isAdmin &&
            hasAValidJetpackConnectionForBlaze() &&
            site.canBlaze &&
            ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.Blaze)
    }

    /**
     * In order for Blaze to work, the site requires the Jetpack Sync module to be enabled. This means not all
     * Jetpack connection will work. For now, Blaze will only be enabled for sites with Jetpack plugin installed and
     * active, or for sites with Blaze for WooCommerce plugin installed and connected.
     */
    private fun hasAValidJetpackConnectionForBlaze() =
        selectedSite.connectionType == Jetpack || isBlazeForWooCommercePluginActive()

    private fun isBlazeForWooCommercePluginActive(): Boolean =
        selectedSite.get().activeJetpackConnectionPlugins?.contains(BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG) == true
}
