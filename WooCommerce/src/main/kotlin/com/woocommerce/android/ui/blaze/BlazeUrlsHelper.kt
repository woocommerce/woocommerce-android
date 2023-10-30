package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class BlazeUrlsHelper @Inject constructor(
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val HTTP_PATTERN = "(https?://)"
        private const val BASE_URL = "https://wordpress.com/advertising"
        private const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL/%s?blazepress-widget=post-%d&_source=%s"
        private const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL/%s?_source=%s"

        // Analytics
        const val BLAZEPRESS_WIDGET = "blazepress-widget"
    }

    fun buildUrlForSite(source: BlazeFlowSource): String {
        return BLAZE_CREATION_FLOW_SITE.format(getSiteUrl(), source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        return BLAZE_CREATION_FLOW_PRODUCT.format(getSiteUrl(), productId, source.trackingName)
    }

    fun buildCampaignsListUrl(): String = "$BASE_URL/campaigns/${getSiteUrl()}"

    fun buildCampaignDetailsUrl(campaignId: Int): String = "$BASE_URL/campaigns/$campaignId/${getSiteUrl()}"

    private fun getSiteUrl() = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")

    enum class BlazeFlowSource(val trackingName: String) {
        MORE_MENU_ITEM("menu"),
        PRODUCT_DETAIL_PROMOTE_BUTTON("product_detail_promote_button"),
        MY_STORE_SECTION("my_store_section"),
        CAMPAIGN_LIST("campaign_list"),
        INTRO_VIEW("intro_view"),
    }
}
