package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class BlazeUrlsHelper @Inject constructor(
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val HTTP_PATTERN = "(https?://)"
        private const val BASE_URL = "https://wordpress.com/advertising/"
        private const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        private const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"

        // Analytics
        const val BLAZEPRESS_WIDGET = "blazepress-widget"
    }

    fun buildUrlForSite(source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_SITE.format(siteUrlWithoutProtocol, source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_PRODUCT.format(siteUrlWithoutProtocol, productId, source.trackingName)
    }

    fun buildCampaignsListUrl(): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return "${BASE_URL}campaigns/$siteUrlWithoutProtocol"
    }

    fun buildCampaignDetailsUrl(campaignId: Int): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return "${BASE_URL}campaigns/$campaignId/$siteUrlWithoutProtocol"
    }

    enum class BlazeFlowSource(val trackingName: String) {
        PRODUCT_DETAIL_OVERFLOW_MENU("product_more_menu"),
        MORE_MENU_ITEM("menu"),
        MY_STORE_BANNER("my_store_banner"),
        PRODUCT_LIST_BANNER("product_list_banner"),
    }
}
