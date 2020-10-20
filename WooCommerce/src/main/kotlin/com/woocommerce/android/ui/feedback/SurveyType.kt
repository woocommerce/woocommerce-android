package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.CROWDSIGNAL_PLATFORM_TAG

enum class SurveyType(private val untagedUrl: String) {
    PRODUCT(AppUrls.CROWDSIGNAL_PRODUCT_SURVEY),
    SHIPPING_LABELS(AppUrls.CROWDSIGNAL_SHIPPING_LABELS_SURVEY),
    MAIN(AppUrls.CROWDSIGNAL_MAIN_SURVEY);

    val url
        get() = "$untagedUrl?$CROWDSIGNAL_PLATFORM_TAG"
}
