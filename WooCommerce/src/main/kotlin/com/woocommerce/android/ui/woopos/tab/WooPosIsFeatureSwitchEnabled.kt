package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * Reads the store's "Point of Sale" feature switch, which merchants toggle under
 * WooCommerce > Settings > Advanced > Features on WooCommerce 10.0 and above.
 *
 * The value rides on the system status report as `settings.enabled_features`, the same field iOS
 * reads. A missing field is reported as a failure rather than "off", so a store is never blocked
 * because the report could not be read.
 *
 * The report is read through [WooCommerceStore.fetchSitePluginsAndSettings], which asks for the
 * plugins and `settings` in one request, so this never costs more than the plugin check already
 * does. Callers that already read a report pass it as [report] so it is not fetched twice, even
 * when that report left the field out.
 *
 * The report is a slow endpoint and this runs on the POS launch path, so the last value read for a
 * site is kept in prefs. Launches answer from that value and refresh it in the background, which
 * leaves it at most one launch behind.
 *
 * Runs on the caller's context; every call it makes switches to its own dispatcher.
 */
class WooPosIsFeatureSwitchEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefs: AppPrefs,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    suspend operator fun invoke(
        forceRefresh: Boolean,
        report: WooPosSystemStatusReport? = null,
    ): Result<Boolean> {
        val site = selectedSite.getOrNull()
            ?: return Result.failure(WooPosCouldNotDetermineValueException())

        // The caller already read a report. Whether or not it carried the field, that is the
        // answer this request has, so fetching the same report again would add nothing.
        report?.let { return store(site, it.enabledFeatures) }

        if (!forceRefresh) {
            appPrefs.getPOSFeatureSwitchEnabledForSite(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )?.let { stored ->
                appCoroutineScope.launch { store(site, loadEnabledFeatures(site)) }
                return Result.success(stored)
            }
        }

        return store(site, loadEnabledFeatures(site))
    }

    private fun store(site: SiteModel, enabledFeatures: List<String>?): Result<Boolean> {
        if (enabledFeatures == null) return Result.failure(WooPosCouldNotDetermineValueException())

        val isEnabled = POINT_OF_SALE_FEATURE in enabledFeatures
        appPrefs.setPOSFeatureSwitchEnabledForSite(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            enabled = isEnabled
        )
        return Result.success(isEnabled)
    }

    private suspend fun loadEnabledFeatures(site: SiteModel): List<String>? =
        wooCommerceStore.fetchSitePluginsAndSettings(site).model?.enabledFeatures

    private companion object {
        const val POINT_OF_SALE_FEATURE = "point_of_sale"
    }
}
