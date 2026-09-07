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
 * When it reads remotely it asks for the system status report's `settings` too and hands the report
 * back on [WooPosWooCorePluginStatus.Active.report], so the POS feature switch is read from the same
 * request. iOS reads both from one report in `loadWooCommercePluginAndPOSFeatureSwitch`, and the
 * report is expensive enough that asking for it twice is worth avoiding.
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
            statusOf(fetched.plugins, report = WooPosSystemStatusReport(fetched.enabledFeatures))
        } else {
            val plugins = wooCommerceStore.getSitePlugins(site)

            // Nothing has ever been synced for this site, so the plugin's absence proves nothing.
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

/**
 * The system status report the plugin state was read from.
 *
 * @param enabledFeatures the report's `settings.enabled_features`, or null when the report left the
 * field out. A report that answered without the field is still an answer, so holding it here keeps
 * [WooPosIsFeatureSwitchEnabled] from asking for the same report again.
 */
data class WooPosSystemStatusReport(val enabledFeatures: List<String>?)

sealed interface WooPosWooCorePluginStatus {
    /**
     * @param report the system status report this state was read from, or null when it came from
     * local data. [WooPosIsFeatureSwitchEnabled] reads the POS feature switch out of it instead of
     * fetching the report again.
     */
    data class Active(
        val version: String,
        val report: WooPosSystemStatusReport? = null,
    ) : WooPosWooCorePluginStatus

    data object NotInstalledOrInactive : WooPosWooCorePluginStatus
    data object CouldNotDetermine : WooPosWooCorePluginStatus
}
