package com.woocommerce.android.ui.woopos.tab

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import javax.inject.Inject

/**
 * Reads the store's "Point of Sale" feature switch, which merchants toggle under
 * WooCommerce > Settings > Advanced > Features on WooCommerce 10.0 and above.
 *
 * The value rides on the system status report as `settings.enabled_features`, the same field iOS
 * reads. A missing field is reported as a failure rather than "off", so a store is never blocked
 * because the report could not be read.
 */
class WooPosIsFeatureSwitchEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ssrFetcher: WCSSRModelCachingFetcher,
    private val gson: Gson,
) {
    suspend operator fun invoke(forceRefresh: Boolean): Result<Boolean> {
        val enabledFeatures = loadEnabledFeatures(forceRefresh)
            ?: return Result.failure(WooPosCouldNotDetermineValueException())

        return Result.success(enabledFeatures.contains(POINT_OF_SALE_FEATURE))
    }

    private suspend fun loadEnabledFeatures(forceRefresh: Boolean): List<*>? {
        val site = selectedSite.getOrNull() ?: return null

        val result = ssrFetcher.load(site, forceRefresh)
        if (result.isError) return null

        val settings = result.model?.settings ?: return null

        return runCatching {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val settingsMap: Map<String, Any> = gson.fromJson(settings, type)
            settingsMap[ENABLED_FEATURES_KEY] as? List<*>
        }.getOrNull()
    }

    private companion object {
        const val ENABLED_FEATURES_KEY = "enabled_features"
        const val POINT_OF_SALE_FEATURE = "point_of_sale"
    }
}
