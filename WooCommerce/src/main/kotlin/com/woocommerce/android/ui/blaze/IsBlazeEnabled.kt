package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.blaze.BlazeStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeStore: BlazeStore
) {
    companion object {
        private const val BASE_URL = "https://wordpress.com/advertising/"
        const val BLAZE_CREATION_FLOW_PRODUCT = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"
        const val HTTP_PATTERN = "(https?://)"
    }

    private var blazeStatus = false

    operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled() && blazeStatus

    suspend fun fetchBlazeStatus() {
        blazeStore.fetchBlazeStatus(selectedSite.get())
        blazeStore.getBlazeStatus(selectedSite.get().siteId)
            .map { it.model?.firstOrNull() }
            .collect {
                blazeStatus = it?.isEligible ?: false
            }
    }

    fun buildUrlForSite(source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_SITE.format(siteUrlWithoutProtocol, source.trackingName)
    }

    fun buildUrlForProduct(productId: Long, source: BlazeFlowSource): String {
        val siteUrlWithoutProtocol = selectedSite.get().url.replace(Regex(HTTP_PATTERN), "")
        return BLAZE_CREATION_FLOW_PRODUCT.format(siteUrlWithoutProtocol, productId, source.trackingName)
    }

    enum class BlazeFlowSource(val trackingName: String) {
        PRODUCT_DETAIL_OVERFLOW_MENU("product_more_menu"),
        MORE_MENU_ITEM("menu"),
    }
}
