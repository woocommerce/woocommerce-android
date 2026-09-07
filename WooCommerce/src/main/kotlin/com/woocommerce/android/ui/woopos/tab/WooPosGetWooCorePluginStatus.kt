package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.matches
import javax.inject.Inject

/**
 * Resolves the state of the WooCommerce core plugin on the selected site.
 *
 * Unlike [com.woocommerce.android.util.GetWooCorePluginCachedVersion], this separates "the plugin is
 * missing or deactivated" from "we could not reach the site", so POS can report the two apart the
 * way iOS does.
 *
 * When it reads remotely it asks for the system status report's `settings` too and hands it back on
 * [WooPosWooCorePluginStatus.Active.systemStatusSettings], so the POS feature switch is read from
 * the same request. iOS reads both from one report in `loadWooCommercePluginAndPOSFeatureSwitch`,
 * and the report is expensive enough that asking for it twice is worth avoiding.
 *
 * Runs on the caller's context; every call it makes switches to its own dispatcher.
 */
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

            // The report answered, so an absent plugin is the site's actual state.
            statusOf(fetched.plugins, systemStatusSettings = fetched.settings)
        } else {
            val plugins = wooCommerceStore.getSitePlugins(site)

            // Nothing has ever been synced for this site, so the plugin's absence proves nothing.
            if (plugins.isEmpty()) return WooPosWooCorePluginStatus.CouldNotDetermine

            statusOf(plugins, systemStatusSettings = null)
        }
    }

    private fun statusOf(
        plugins: List<SitePluginModel>,
        systemStatusSettings: String?
    ): WooPosWooCorePluginStatus {
        val wooCore = plugins.firstOrNull { it.matches(WooCommerceStore.WooPlugin.WOO_CORE) }
            ?: return WooPosWooCorePluginStatus.NotInstalledOrInactive

        return if (wooCore.isActive) {
            WooPosWooCorePluginStatus.Active(wooCore.version, systemStatusSettings)
        } else {
            WooPosWooCorePluginStatus.NotInstalledOrInactive
        }
    }
}

sealed interface WooPosWooCorePluginStatus {
    /**
     * @param systemStatusSettings the `settings` object of the system status report this state was
     * read from, or null when it came from local data. [WooPosIsFeatureSwitchEnabled] reads the POS
     * feature switch out of it instead of fetching the report again.
     */
    data class Active(
        val version: String,
        val systemStatusSettings: String? = null,
    ) : WooPosWooCorePluginStatus

    data object NotInstalledOrInactive : WooPosWooCorePluginStatus
    data object CouldNotDetermine : WooPosWooCorePluginStatus
}
