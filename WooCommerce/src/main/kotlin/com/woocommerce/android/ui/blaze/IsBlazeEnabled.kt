package com.woocommerce.android.ui.blaze

import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
) {
    companion object {
        const val HTTP_PATTERN = "(https?://)"
        private const val BASE_URL = "https://wordpress.com/advertising/"
        const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"

        // Analytics
        const val BLAZEPRESS_WIDGET = "blazepress-widget"
    }

    suspend operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled() &&
        selectedSite.getIfExists()?.isAdmin ?: false &&
        selectedSite.getIfExists()?.canBlaze ?: false &&
        selectedSite.getIfExists()?.isSitePublic ?: false &&
        isRemoteFeatureFlagEnabled(WOO_BLAZE)

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
