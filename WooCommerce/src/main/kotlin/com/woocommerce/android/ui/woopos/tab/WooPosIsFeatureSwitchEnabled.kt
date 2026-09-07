package com.woocommerce.android.ui.woopos.tab

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

/**
 * Reads the store's "Point of Sale" feature switch, which merchants toggle under
 * WooCommerce > Settings > Advanced > Features on WooCommerce 10.0 and above.
 *
 * The value rides on the system status report as `settings.enabled_features`, the same field iOS
 * reads. A missing field is reported as a failure rather than "off", so a store is never blocked
 * because the report could not be read.
 *
 * The report is a slow endpoint and this runs on the POS launch path, so the last value read for a
 * site is kept in prefs. Launches answer from that value and refresh it in the background, which
 * leaves it at most one launch behind. Callers that already hold a freshly fetched report pass it
 * as [systemStatusSettings] so it is not fetched twice.
 *
 * Runs on the caller's context; every call it makes switches to its own dispatcher.
 */
class WooPosIsFeatureSwitchEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ssrFetcher: WCSSRModelCachingFetcher,
    private val gson: Gson,
    private val appPrefs: AppPrefs,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    suspend operator fun invoke(
        forceRefresh: Boolean,
        systemStatusSettings: String? = null,
    ): Result<Boolean> {
        val site = selectedSite.getOrNull()
            ?: return Result.failure(WooPosCouldNotDetermineValueException())

        systemStatusSettings?.let { settings ->
            return store(site, parseEnabledFeatures(settings))
        }

        if (!forceRefresh) {
            appPrefs.getPOSFeatureSwitchEnabledForSite(site.siteId)?.let { stored ->
                appCoroutineScope.launch { store(site, loadEnabledFeatures(site, forceRefresh = false)) }
                return Result.success(stored)
            }
        }

        return store(site, loadEnabledFeatures(site, forceRefresh))
    }

    private fun store(site: SiteModel, enabledFeatures: List<*>?): Result<Boolean> {
        if (enabledFeatures == null) return Result.failure(WooPosCouldNotDetermineValueException())

        val isEnabled = enabledFeatures.contains(POINT_OF_SALE_FEATURE)
        appPrefs.setPOSFeatureSwitchEnabledForSite(site.siteId, isEnabled)
        return Result.success(isEnabled)
    }

    private suspend fun loadEnabledFeatures(site: SiteModel, forceRefresh: Boolean): List<*>? {
        val result = ssrFetcher.load(site, forceRefresh)
        if (result.isError) return null

        return result.model?.settings?.let { parseEnabledFeatures(it) }
    }

    private fun parseEnabledFeatures(settings: String): List<*>? = runCatching {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val settingsMap: Map<String, Any> = gson.fromJson(settings, type)
        settingsMap[ENABLED_FEATURES_KEY] as? List<*>
    }.getOrNull()

    private companion object {
        const val ENABLED_FEATURES_KEY = "enabled_features"
        const val POINT_OF_SALE_FEATURE = "point_of_sale"
    }
}
