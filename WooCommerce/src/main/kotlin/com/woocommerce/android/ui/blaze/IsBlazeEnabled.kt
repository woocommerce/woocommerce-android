package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.Jetpack
import com.woocommerce.android.tools.SiteConnectionType.JetpackConnectionPackage
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled
) {
    companion object {
        private const val BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG = "blaze-ads"
    }

    suspend operator fun invoke(): Boolean = selectedSite.getIfExists()?.isAdmin ?: false &&
        hasAValidJetpackConnectionForBlaze() &&
        selectedSite.getIfExists()?.canBlaze ?: false &&
        isRemoteFeatureFlagEnabled(WOO_BLAZE)

    /**
     * In order for Blaze to work, the site requires the Jetpack Sync module to be enabled. This means not all
     * Jetpack connection will work. For now, Blaze will only be enabled for sites with Jetpack plugin installed and
     * active, or for sites with Blaze for WooCommerce plugin installed and connected (Jetpack CP with sync module)
     */
    private fun hasAValidJetpackConnectionForBlaze() =
        selectedSite.connectionType == Jetpack ||
            (selectedSite.connectionType == JetpackConnectionPackage && isBlazeForWooCommercePluginInstalled())

    private fun isBlazeForWooCommercePluginInstalled(): Boolean =
        selectedSite.get().activeJetpackConnectionPlugins.any { it.toString() == BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG }
}
