package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.CROWDSIGNAL_PLATFORM_TAG

enum class SurveyType(private val untagedUrl: String, private val milestone: Int? = null) {
    PRODUCT(AppUrls.CROWDSIGNAL_PRODUCT_SURVEY, 4),
    SHIPPING_LABELS(AppUrls.CROWDSIGNAL_SHIPPING_LABELS_SURVEY, 1),
    MAIN(AppUrls.CROWDSIGNAL_MAIN_SURVEY);

    val url
        get() = "$untagedUrl?$CROWDSIGNAL_PLATFORM_TAG$milestoneTag"

    private val milestoneTag
        get() = milestone?.let {
            "&milestone=$it"
        } ?: ""
}
