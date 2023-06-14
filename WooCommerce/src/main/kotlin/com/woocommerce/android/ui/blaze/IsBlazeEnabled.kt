package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val BASE_URL = "https://wordpress.com/advertising/"

        const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"
        const val HTTP_PATTERN = "(https?://)"
    }

    operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled()

    fun buildUrlForSite(source: BlazeFlowSource): String {
        val siteUrl = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_SITE.format(siteUrl, source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        val siteUrl = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_PRODUCT.format(siteUrl, productId, source.trackingName)
    }

    enum class BlazeFlowSource(val trackingName: String) {
        PRODUCT_DETAIL_OVERFLOW_MENU("product_more_menu"),
        MORE_MENU_ITEM("menu"),
    }
}
