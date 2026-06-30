package com.woocommerce.android.config

import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin
import javax.inject.Inject

class WPComMobileActivePluginVersionsProvider @Inject constructor() {
    fun buildActivePluginVersions(plugins: List<SitePluginModel>?): Map<String, String> {
        val wooCorePlugin = plugins.orEmpty().firstOrNull { plugin ->
            plugin.isActive &&
                plugin.name == WooPlugin.WOO_CORE.pluginName &&
                plugin.version.isNotBlank()
        }

        return wooCorePlugin?.let { plugin ->
            mapOf(WOO_CORE_PLUGIN_PATH to plugin.version)
        }.orEmpty()
    }

    private companion object {
        const val WOO_CORE_PLUGIN_PATH = "woocommerce/woocommerce.php"
    }
}
