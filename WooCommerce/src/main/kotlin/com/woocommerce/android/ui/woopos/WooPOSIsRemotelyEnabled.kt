package com.woocommerce.android.ui.woopos

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.woocommerce.android.cache.SSRCache
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPOSIsRemotelyEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {

    suspend operator fun invoke(): Boolean {
        val result = SSRCache.load(selectedSite.get(), wooCommerceStore)

        if (!result.isError) {
            result.model?.let { ssr ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val settingsMap: Map<String, Any> = Gson().fromJson(ssr.settings, type)
                val enabledFeatures = settingsMap["enabled_features"] as? List<*>

                return enabledFeatures?.contains("point_of_sale") == true
            }
        }
        return false
    }
}
