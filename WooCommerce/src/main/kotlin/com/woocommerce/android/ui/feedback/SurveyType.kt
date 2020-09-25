package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls

enum class SurveyType(val url: String) {
    PRODUCT(AppUrls.CROWDSIGNAL_PRODUCT_SURVEY),
    MAIN(AppUrls.CROWDSIGNAL_MAIN_SURVEY)
}
