package com.woocommerce.android.ui.google

import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.features.SSRFeatureChecker
import org.json.JSONArray
import org.wordpress.android.fluxc.model.WCSSRModel
import javax.inject.Inject

class GoogleFeatureChecker @Inject constructor(
    private val googleRepository: GoogleRepository
) : SSRFeatureChecker {

    companion object {
        private const val GOOGLE_FOR_WOO_PLUGIN_NAME = "google-listings-and-ads/google-listings-and-ads.php"
        private const val GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION = "2.7.7"
    }

    override suspend fun check(settings: WCSSRModel): Boolean {
        return if (isGoogleForWooPluginEligible(JSONArray(settings.activePlugins))) {
            googleRepository.isGoogleAdsAccountConnected()
        } else {
            false
        }
    }

    private fun isGoogleForWooPluginEligible(activePlugins: JSONArray): Boolean {
        for (i in 0 until activePlugins.length()) {
            val plugin = activePlugins.getJSONObject(i)
            val currentPluginName = plugin.optString("plugin")
            val currentPluginVersion = plugin.optString("version")
            if (currentPluginName == GOOGLE_FOR_WOO_PLUGIN_NAME &&
                currentPluginVersion.isVersionAtLeast(GOOGLE_FOR_WOO_PLUGIN_MIN_VERSION)
            ) {
                return true
            }
        }
        return false
    }
}
