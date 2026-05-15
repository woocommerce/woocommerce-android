package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel

internal class WooAiSmokePreflight(
    private val selectedSite: SelectedSite,
) {
    fun requireReady(): SiteModel {
        val site = selectedSite.getOrNull()
            ?: error("Woo AI smoke requires an already authenticated app with a selected site.")
        require(site.siteId > 0L) {
            "Woo AI smoke selected site must have a remote siteId."
        }
        return site
    }
}
