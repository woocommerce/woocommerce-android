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
