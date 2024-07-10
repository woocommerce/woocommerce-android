package com.woocommerce.android.ui.google

import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.util.FeatureFlag
import org.json.JSONArray
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsGoogleForWooEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val googleRepository: GoogleRepository,
    private val pluginRepository: PluginRepository,
    private val wooCommerceStore: WooCommerceStore
) {

    companion object {
        private const val GOOGLE_FOR_WOO_PLUGIN_NAME = "google-listings-and-ads/google-listings-and-ads.php"
        private const val GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION = "2.7.5"
    }

    suspend operator fun invoke(): Boolean {
        val result = wooCommerceStore.fetchSSR(selectedSite.get())

        if (!result.isError) {
            result.model?.let { ssr ->
                if (isGoogleForWooPluginEligible(JSONArray(ssr.activePlugins))) {
                    return FeatureFlag.GOOGLE_ADS_M1.isEnabled() && googleRepository.isGoogleAdsAccountConnected()
                }
            }
        }
        return false
    }

    private fun isGoogleForWooPluginEligible(activePlugins: JSONArray): Boolean {
        for (i in 0 until activePlugins.length()) {
            val plugin = activePlugins.getJSONObject(i)
            val currentPluginName = plugin.optString("plugin")
            val currentPluginVersion = plugin.optString("version")
            if (currentPluginName == GOOGLE_FOR_WOO_PLUGIN_NAME &&
                currentPluginVersion.isVersionAtLeast(GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION)) {
                return true
            }
        }
        return false
    }
}
