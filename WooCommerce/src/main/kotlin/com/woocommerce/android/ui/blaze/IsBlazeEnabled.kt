package com.woocommerce.android.ui.blaze

import com.woocommerce.android.model.UserRole.Administrator
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val userEligibilityFetcher: UserEligibilityFetcher,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
) {
    companion object {
        private const val BASE_URL = "https://wordpress.com/advertising/"
        const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"
        const val HTTP_PATTERN = "(https?://)"
    }

    suspend operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled() &&
        userIsAdminForCurrentSite() &&
        selectedSite.getIfExists()?.canBlaze ?: false &&
        isRemoteFeatureFlagEnabled(WOO_BLAZE)

    fun buildUrlForSite(source: BlazeFlowSource): String {
        val siteUrl = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_SITE.format(siteUrl, source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        val siteUrl = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_PRODUCT.format(siteUrl, productId, source.trackingName)
    }

    private fun userIsAdminForCurrentSite() =
        selectedSite.exists() &&
            userEligibilityFetcher.getUser()?.roles?.any { it is Administrator } ?: false

    enum class BlazeFlowSource(val trackingName: String) {
        PRODUCT_DETAIL_OVERFLOW_MENU("product_more_menu"),
        MORE_MENU_ITEM("menu"),
    }
}
