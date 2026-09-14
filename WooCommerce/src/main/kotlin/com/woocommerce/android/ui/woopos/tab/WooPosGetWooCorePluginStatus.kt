package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.EnabledFeatures
import org.wordpress.android.fluxc.store.matches
import javax.inject.Inject

class WooPosGetWooCorePluginStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    suspend operator fun invoke(refreshPolicy: WooPosLaunchabilityRefreshPolicy): WooPosWooCorePluginStatus {
        val site = selectedSite.getOrNull() ?: return WooPosWooCorePluginStatus.CouldNotDetermine

        return if (refreshPolicy == WooPosLaunchabilityRefreshPolicy.ForceRefresh) {
            fetchedStatus(site)
        } else {
            storedStatus(site)
        }
    }

    private suspend fun storedStatus(site: SiteModel): WooPosWooCorePluginStatus {
        // The stored list can be stale or partial, so only a fetch may report the plugin as missing.
        val wooCore = activePlugin(wooCommerceStore.getSitePlugins(site))
            ?: return WooPosWooCorePluginStatus.CouldNotDetermine

        return WooPosWooCorePluginStatus.Active(wooCore.version, report = null)
    }

    private suspend fun fetchedStatus(site: SiteModel): WooPosWooCorePluginStatus {
        val result = wooCommerceStore.fetchSitePluginsAndSettings(site)

        // The wc/v3 routes are gone while WooCommerce is deactivated, so a missing route is the plugin's state.
        if (result.error?.type == WooErrorType.API_NOT_FOUND) return WooPosWooCorePluginStatus.NotInstalledOrInactive

        val fetched = result.model ?: return WooPosWooCorePluginStatus.CouldNotDetermine

        val wooCore = activePlugin(fetched.plugins)
            ?: return WooPosWooCorePluginStatus.NotInstalledOrInactive

        return WooPosWooCorePluginStatus.Active(wooCore.version, WooPosSystemStatusReport(fetched.enabledFeatures))
    }

    // Another plugin can share WooCommerce's file name, and the stored list is sorted by name,
    // so an inactive one can come first. Only an active match is WooCommerce.
    private fun activePlugin(plugins: List<SitePluginModel>): SitePluginModel? =
        plugins.firstOrNull { it.matches(WooCommerceStore.WooPlugin.WOO_CORE) && it.isActive }
}

data class WooPosSystemStatusReport(val enabledFeatures: EnabledFeatures)

sealed interface WooPosWooCorePluginStatus {
    data class Active(
        val version: String,
        val report: WooPosSystemStatusReport? = null,
    ) : WooPosWooCorePluginStatus

    data object NotInstalledOrInactive : WooPosWooCorePluginStatus
    data object CouldNotDetermine : WooPosWooCorePluginStatus
}
