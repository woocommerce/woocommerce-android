package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.matches
import javax.inject.Inject

class WooPosGetWooCorePluginStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    suspend operator fun invoke(forceRefresh: Boolean): WooPosWooCorePluginStatus {
        val site = selectedSite.getOrNull() ?: return WooPosWooCorePluginStatus.CouldNotDetermine

        return if (forceRefresh) {
            val result = wooCommerceStore.fetchSitePluginsAndSettings(site)
            val fetched = result.model
                ?: return WooPosWooCorePluginStatus.CouldNotDetermine

            statusOf(fetched.plugins, report = WooPosSystemStatusReport(fetched.enabledFeatures))
        } else {
            val plugins = wooCommerceStore.getSitePlugins(site)

            if (plugins.isEmpty()) return WooPosWooCorePluginStatus.CouldNotDetermine

            statusOf(plugins, report = null)
        }
    }

    private fun statusOf(
        plugins: List<SitePluginModel>,
        report: WooPosSystemStatusReport?
    ): WooPosWooCorePluginStatus {
        val wooCore = plugins.firstOrNull { it.matches(WooCommerceStore.WooPlugin.WOO_CORE) }
            ?: return WooPosWooCorePluginStatus.NotInstalledOrInactive

        return if (wooCore.isActive) {
            WooPosWooCorePluginStatus.Active(wooCore.version, report)
        } else {
            WooPosWooCorePluginStatus.NotInstalledOrInactive
        }
    }
}

/** @param enabledFeatures null when the report left the field out, which is not the same as off. */
data class WooPosSystemStatusReport(val enabledFeatures: List<String>?)

sealed interface WooPosWooCorePluginStatus {
    data class Active(
        val version: String,
        val report: WooPosSystemStatusReport? = null,
    ) : WooPosWooCorePluginStatus

    data object NotInstalledOrInactive : WooPosWooCorePluginStatus
    data object CouldNotDetermine : WooPosWooCorePluginStatus
}
