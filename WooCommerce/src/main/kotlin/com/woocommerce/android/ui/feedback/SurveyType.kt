package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls
import com.woocommerce.android.BuildConfig

@Suppress("MagicNumber")
enum class SurveyType(private val untaggedUrl: String, private val milestone: Int? = null) {
    PRODUCT(AppUrls.CROWDSIGNAL_PRODUCT_SURVEY, 4),
    MAIN(AppUrls.CROWDSIGNAL_MAIN_SURVEY),
    ADDONS(AppUrls.ADDONS_SURVEY),
    STORE_ONBOARDING(AppUrls.CROWDSIGNAL_STORE_SETUP_SURVEY),
    ANALYTICS_HUB(AppUrls.CROWDSIGNAL_ANALYTICS_HUB_SURVEY),
    ORDER_SHIPPING_LINES(AppUrls.CROWDSIGNAL_ORDER_SHIPPING_LINES_SURVEY),
    WOO_POS_POTENTIAL_USER(AppUrls.CROWDSIGNAL_WOO_POS_SURVEY_POTENTIAL_USER),
    WOO_POS_CURRENT_USER(AppUrls.CROWDSIGNAL_WOO_POS_SURVEY_CURRENT_USER);

    val url
        get() = "$untaggedUrl?$platformTag$appVersionTag$milestoneTag"

    private val milestoneTag
        get() = when (this) {
            PRODUCT -> "&product-milestone=$milestone"
            else -> ""
        }

    private val appVersionTag = "&app-version=${BuildConfig.VERSION_NAME}"

    private val platformTag = "woo-mobile-platform=android"
}
