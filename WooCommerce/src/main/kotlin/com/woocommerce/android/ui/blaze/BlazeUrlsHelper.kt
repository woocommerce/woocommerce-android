package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class BlazeUrlsHelper @Inject constructor(
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val HTTP_PATTERN = "(https?://)"
        const val BASE_URL = "https://wordpress.com/advertising"
        const val PROMOTE_AGAIN_URL_PATH = "blazepress-widget=post"
    }

    fun buildCampaignsListUrl(): String = "$BASE_URL/campaigns/${getSiteUrl()}"

    fun buildCampaignDetailsUrl(campaignId: String): String = "$BASE_URL/campaigns/$campaignId/${getSiteUrl()}"

    fun getCampaignStopUrlPath(campaignId: String): String = "/campaigns/$campaignId/stop"

    private fun getSiteUrl() = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")

    fun extractProductIdFromPromoteAgainUrl(url: String): Long? =
        url.substringAfter("post-")
            .substringBefore("_campaign")
            .toLongOrNull()

    enum class BlazeFlowSource(val trackingName: String) {
        MORE_MENU_ITEM("menu"),
        PRODUCT_DETAIL_PROMOTE_BUTTON("product_detail_promote_button"),
        MY_STORE_SECTION("my_store_section"),
        LOCAL_NOTIFICATION_NO_CAMPAIGN_REMINDER("local_notification_no_campaign_reminder"),
        CAMPAIGN_LIST("campaign_list"),
        INTRO_VIEW("intro_view"),
    }
}
