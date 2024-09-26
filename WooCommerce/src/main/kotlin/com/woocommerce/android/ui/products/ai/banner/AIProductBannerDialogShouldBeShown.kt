package com.woocommerce.android.ui.products.ai.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class AIProductBannerDialogShouldBeShown @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    operator fun invoke(): Boolean {
        return selectedSite.getOrNull()?.isEligibleForAI == true &&
            !appPrefsWrapper.wasAIProductDescriptionPromoDialogShown
    }
}
