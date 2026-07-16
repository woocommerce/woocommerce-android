package com.woocommerce.android.ui.blaze

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.Jetpack
import com.woocommerce.android.tools.connectionType
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite
) {
    companion object {
        @VisibleForTesting
        const val BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG = "blaze-ads"
    }

    operator fun invoke(): Boolean {
        val site = selectedSite.getOrNull() ?: return false
        return site.isAdmin &&
            site.hasAValidJetpackConnectionForBlaze() &&
            site.canBlaze
    }

    /**
     * In order for Blaze to work, the site requires the Jetpack Sync module to be enabled. This means not all
     * Jetpack connection will work. For now, Blaze will only be enabled for sites with Jetpack plugin installed and
     * active, or for sites with Blaze for WooCommerce plugin installed and connected.
     */
    private fun SiteModel.hasAValidJetpackConnectionForBlaze() =
        connectionType == Jetpack || isBlazeForWooCommercePluginActive()

    private fun SiteModel.isBlazeForWooCommercePluginActive(): Boolean =
        activeJetpackConnectionPlugins?.contains(BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG) == true
}
