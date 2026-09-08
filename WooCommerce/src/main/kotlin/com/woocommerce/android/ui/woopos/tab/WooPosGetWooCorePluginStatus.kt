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
        val plugins = wooCommerceStore.getSitePlugins(site)

        if (plugins.isEmpty()) return WooPosWooCorePluginStatus.CouldNotDetermine

        return statusOf(plugins, report = null)
    }

    private suspend fun fetchedStatus(site: SiteModel): WooPosWooCorePluginStatus {
        val result = wooCommerceStore.fetchSitePluginsAndSettings(site)

        // The wc/v3 routes are gone while WooCommerce is deactivated, so a missing route is the plugin's state.
        if (result.error?.type == WooErrorType.API_NOT_FOUND) return WooPosWooCorePluginStatus.NotInstalledOrInactive

        val fetched = result.model ?: return WooPosWooCorePluginStatus.CouldNotDetermine

        return statusOf(fetched.plugins, report = WooPosSystemStatusReport(fetched.enabledFeatures))
    }

    private fun statusOf(
        plugins: List<SitePluginModel>,
        report: WooPosSystemStatusReport?
    ): WooPosWooCorePluginStatus {
        // Another plugin can share WooCommerce's file name, and the stored list is sorted by name,
        // so an inactive one can come first. Only an active match is WooCommerce.
        val wooCore = plugins.firstOrNull { it.matches(WooCommerceStore.WooPlugin.WOO_CORE) && it.isActive }
            ?: return WooPosWooCorePluginStatus.NotInstalledOrInactive

        return WooPosWooCorePluginStatus.Active(wooCore.version, report)
    }
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
