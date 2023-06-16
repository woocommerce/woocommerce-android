package com.woocommerce.android.ui.blaze

import com.woocommerce.android.model.UserRole.Administrator
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val userEligibilityFetcher: UserEligibilityFetcher
) {
    companion object {
        private const val BASE_URL = "https://wordpress.com/advertising/"
        const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"
        const val HTTP_PATTERN = "(https?://)"
    }

    operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled() &&
        hasAdministratorRole() &&
        selectedSite.getIfExists()?.canBlaze ?: false

    fun buildUrlForSite(source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_SITE.format(siteUrlWithoutProtocol, source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_PRODUCT.format(siteUrlWithoutProtocol, productId, source.trackingName)
    }

    private fun hasAdministratorRole() =
        selectedSite.exists() &&
            userEligibilityFetcher.getUser()?.roles?.any { it is Administrator } ?: false

    enum class BlazeFlowSource(val trackingName: String) {
        PRODUCT_DETAIL_OVERFLOW_MENU("product_more_menu"),
        MORE_MENU_ITEM("menu"),
    }
}
