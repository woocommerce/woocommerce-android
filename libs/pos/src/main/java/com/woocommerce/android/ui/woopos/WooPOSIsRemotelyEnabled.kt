package com.woocommerce.android.ui.woopos

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import javax.inject.Inject

class WooPOSIsRemotelyEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ssrFetcher: WCSSRModelCachingFetcher,
    private val gson: Gson
) {

    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Boolean> {
        val result = ssrFetcher.load(selectedSite.get(), forceRefresh)

        if (!result.isError) {
            result.model?.let { ssr ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val settingsMap: Map<String, Any> = gson.fromJson(ssr.settings, type)
                val enabledFeatures = settingsMap["enabled_features"] as? List<*>

                return enabledFeatures?.let {
                    Result.success(it.contains("point_of_sale"))
                } ?: Result.failure(WooPosCouldNotDetermineValueException())
            }
        }
        return Result.failure(WooPosCouldNotDetermineValueException())
    }
}
