package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsTracker

enum class SurveyType(private val untaggedUrl: String) {
    MAIN(AppUrls.CROWDSIGNAL_MAIN_SURVEY),
    ADDONS(AppUrls.ADDONS_SURVEY),
    STORE_ONBOARDING(AppUrls.CROWDSIGNAL_STORE_SETUP_SURVEY),
    ANALYTICS_HUB(AppUrls.CROWDSIGNAL_ANALYTICS_HUB_SURVEY),
    ORDER_SHIPPING_LINES(AppUrls.CROWDSIGNAL_ORDER_SHIPPING_LINES_SURVEY),
    WOO_POS_POTENTIAL_USER(AppUrls.CROWDSIGNAL_WOO_POS_SURVEY_POTENTIAL_USER),
    WOO_POS_CURRENT_USER(AppUrls.CROWDSIGNAL_WOO_POS_SURVEY_CURRENT_USER),
    AI_ASSISTANT(AppUrls.CROWDSIGNAL_AI_ASSISTANT_SURVEY);

    val url
        get() = "$untaggedUrl?$platformTag$appVersionTag"

    val feedbackContext: String
        get() = when (this) {
            MAIN -> AnalyticsTracker.VALUE_FEEDBACK_GENERAL_CONTEXT
            STORE_ONBOARDING -> AnalyticsTracker.VALUE_FEEDBACK_STORE_SETUP_CONTEXT
            ADDONS -> AnalyticsTracker.VALUE_PRODUCT_ADDONS_FEEDBACK
            ANALYTICS_HUB -> AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK
            ORDER_SHIPPING_LINES -> AnalyticsTracker.VALUE_ORDER_SHIPPING_LINES_FEEDBACK
            WOO_POS_POTENTIAL_USER -> AnalyticsTracker.VALUE_WOO_POS_POTENTIAL_USER_FEEDBACK
            WOO_POS_CURRENT_USER -> AnalyticsTracker.VALUE_WOO_POS_CURRENT_USER_FEEDBACK
            AI_ASSISTANT -> AnalyticsTracker.VALUE_AI_ASSISTANT_FEEDBACK
        }

    private val appVersionTag = "&app-version=${BuildConfig.VERSION_NAME}"

    private val platformTag = "woo-mobile-platform=android"
}
