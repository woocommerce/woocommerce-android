package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.EnabledFeatures
import javax.inject.Inject

class WooPosIsFeatureSwitchEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefs: AppPrefs,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    suspend operator fun invoke(
        refreshPolicy: WooPosLaunchabilityRefreshPolicy,
        report: WooPosSystemStatusReport? = null,
    ): Result<Boolean> {
        val site = selectedSite.getOrNull() ?: return couldNotDetermine()

        if (report != null) return store(site, report.enabledFeatures)

        storedValue(site, refreshPolicy)?.let { stored ->
            if (refreshPolicy == WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh) {
                appCoroutineScope.launch { store(site, loadEnabledFeatures(site)) }
            }
            return Result.success(stored)
        }

        return if (refreshPolicy == WooPosLaunchabilityRefreshPolicy.UseCache) {
            couldNotDetermine()
        } else {
            store(site, loadEnabledFeatures(site))
        }
    }

    private fun storedValue(site: SiteModel, refreshPolicy: WooPosLaunchabilityRefreshPolicy): Boolean? =
        if (refreshPolicy == WooPosLaunchabilityRefreshPolicy.ForceRefresh) {
            null
        } else {
            appPrefs.getPOSFeatureSwitchEnabledForSite(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        }

    private fun store(site: SiteModel, enabledFeatures: EnabledFeatures): Result<Boolean> {
        if (enabledFeatures !is EnabledFeatures.Known) return couldNotDetermine()

        val isEnabled = POINT_OF_SALE_FEATURE in enabledFeatures.features
        appPrefs.setPOSFeatureSwitchEnabledForSite(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            enabled = isEnabled
        )
        return Result.success(isEnabled)
    }

    private suspend fun loadEnabledFeatures(site: SiteModel): EnabledFeatures =
        wooCommerceStore.fetchSitePluginsAndSettings(site).model?.enabledFeatures ?: EnabledFeatures.Unknown

    private fun couldNotDetermine(): Result<Boolean> = Result.failure(WooPosCouldNotDetermineValueException())

    private companion object {
        const val POINT_OF_SALE_FEATURE = "point_of_sale"
    }
}
