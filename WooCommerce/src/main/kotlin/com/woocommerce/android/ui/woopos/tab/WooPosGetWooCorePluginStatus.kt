package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * Resolves the state of the WooCommerce core plugin on the selected site.
 *
 * Unlike [com.woocommerce.android.util.GetWooCorePluginCachedVersion], this separates "the plugin is
 * missing or deactivated" from "we could not reach the site", so POS can report the two apart the
 * way iOS does.
 */
class WooPosGetWooCorePluginStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    suspend operator fun invoke(forceRefresh: Boolean): WooPosWooCorePluginStatus =
        withContext(Dispatchers.IO) {
            val site = selectedSite.getOrNull() ?: return@withContext WooPosWooCorePluginStatus.CouldNotDetermine

            val plugins = if (forceRefresh) {
                val result = wooCommerceStore.fetchSitePlugins(site)
                if (result.isError) return@withContext WooPosWooCorePluginStatus.CouldNotDetermine
                result.model.orEmpty()
            } else {
                wooCommerceStore.getSitePlugins(site)
            }

            // Nothing has ever been synced for this site, so the plugin's absence proves nothing.
            if (plugins.isEmpty()) return@withContext WooPosWooCorePluginStatus.CouldNotDetermine

            val wooCore = plugins.firstOrNull { it.matchesWooCore() }
                ?: return@withContext WooPosWooCorePluginStatus.NotInstalledOrInactive

            if (wooCore.isActive) {
                WooPosWooCorePluginStatus.Active(wooCore.version)
            } else {
                WooPosWooCorePluginStatus.NotInstalledOrInactive
            }
        }

    private fun SitePluginModel.matchesWooCore(): Boolean =
        name.substringAfterLast('/') == WOO_CORE_NAME

    private companion object {
        val WOO_CORE_NAME: String =
            WooCommerceStore.WooPlugin.WOO_CORE.pluginName.substringAfterLast('/')
    }
}

sealed interface WooPosWooCorePluginStatus {
    data class Active(val version: String) : WooPosWooCorePluginStatus
    data object NotInstalledOrInactive : WooPosWooCorePluginStatus
    data object CouldNotDetermine : WooPosWooCorePluginStatus
}
