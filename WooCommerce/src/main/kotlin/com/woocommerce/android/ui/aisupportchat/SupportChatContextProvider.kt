package com.woocommerce.android.ui.aisupportchat

import com.google.gson.JsonObject
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SupportChatContextProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun buildInitialContext(): JsonObject {
        val site = selectedSite.get()
        return JsonObject().apply {
            addProperty("platform", "android")
            addProperty("app_version", buildConfigWrapper.versionName)
            addProperty("site_id", site.siteId)
            addProperty("local_site_id", site.id)
            addProperty("site_url", site.url)
        }
    }
}
