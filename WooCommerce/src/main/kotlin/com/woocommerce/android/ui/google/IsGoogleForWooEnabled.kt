package com.woocommerce.android.ui.google

import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsGoogleForWooEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val googleRepository: GoogleRepository,
    private val pluginRepository: PluginRepository
) {

    companion object {
        private const val GOOGLE_FOR_WOO_PLUGIN_NAME = "google-listings-and-ads/google-listings-and-ads"
        private const val GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION = "2.7.5"
    }

    suspend operator fun invoke(): Boolean {
        val plugin = pluginRepository.fetchPlugin(selectedSite.get(), GOOGLE_FOR_WOO_PLUGIN_NAME).getOrNull()

        return if (plugin == null || !plugin.version.isVersionAtLeast(GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION)) {
            false
        } else {
            FeatureFlag.GOOGLE_ADS_M1.isEnabled() && googleRepository.isGoogleAdsAccountConnected()
        }
    }
}
