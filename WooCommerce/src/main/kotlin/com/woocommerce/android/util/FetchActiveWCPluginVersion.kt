package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.matches
import javax.inject.Inject

class FetchActiveWCPluginVersion @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke(): String? = withContext(Dispatchers.IO) {
        selectedSite.getOrNull()?.let { selectedSite ->
            val fetchSitePluginsResult = wooCommerceStore.fetchSitePlugins(selectedSite)

            if (fetchSitePluginsResult.isError) {
                return@withContext null
            }
            val wooPluginInfo = fetchSitePluginsResult.model.getWooPlugin()

            return@withContext wooPluginInfo?.version
        }
    }

    private fun List<SitePluginModel>?.getWooPlugin(): SitePluginModel? =
        this?.firstOrNull { it.matches(WooCommerceStore.WooPlugin.WOO_CORE) && it.isActive }
}
