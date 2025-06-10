package com.woocommerce.android.ui.woopos.common.data

import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.VariationFilterOption
import javax.inject.Inject

class WooPosVariationsTypesFilterConfig @Inject constructor() {
    val filters: Map<VariationFilterOption, String> =
        mapOf(
            VariationFilterOption.STATUS to VARIATION_STATUS_PUBLISH,
            VariationFilterOption.DOWNLOADABLE to DownloadableOptions.FALSE.toString()
        )

    companion object {
        private const val VARIATION_STATUS_PUBLISH = "publish"
    }
}
