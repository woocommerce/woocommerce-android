package com.woocommerce.android.ui.pospromo

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.UtmProvider
import dagger.Reusable
import javax.inject.Inject

@Reusable
class PosPromoUtmProvider @Inject constructor(
    private val selectedSite: SelectedSite,
) {
    fun getUrlWithUtmParams(url: String): String {
        return UtmProvider(
            campaign = UTM_CAMPAIGN,
            source = UTM_SOURCE,
            content = UTM_CONTENT,
            siteId = selectedSite.getIfExists()?.siteId
        ).getUrlWithUtmParams(url)
    }

    companion object {
        private const val UTM_CAMPAIGN = "pos_promotion_modal"
        private const val UTM_SOURCE = "my_store"
        private const val UTM_CONTENT = "pos_promotion_cta"
    }
}
