package com.woocommerce.android

import com.woocommerce.android.support.help.HelpOrigin.LOGIN_EMAIL
import com.woocommerce.android.support.help.HelpOrigin.LOGIN_SITE_ADDRESS
import com.woocommerce.android.support.help.HelpOrigin.LOGIN_USERNAME_PASSWORD

object AppUrls {
    const val APP_HELP_CENTER = "https://woocommerce.com/document/android/"

    const val AUTOMATTIC_TOS = "https://wordpress.com/tos/"
    const val AUTOMATTIC_PRIVACY_POLICY = "https://www.automattic.com/privacy"
    const val AUTOMATTIC_PRIVACY_POLICY_CA = "https://automattic.com/privacy/#california-consumer-privacy-act-ccpa"
    const val AUTOMATTIC_COOKIE_POLICY = "https://www.automattic.com/cookies"
    const val AUTOMATTIC_HIRING = "https://automattic.com/work-with-us"
    const val AUTOMATTIC_AI_GUIDELINES = "https://automattic.com/ai-guidelines/"

    const val WOOCOMMERCE_UPGRADE = "https://woocommerce.com/document/how-to-update-woocommerce/"
    const val WOOCOMMERCE_PLUGIN = "https://wordpress.org/plugins/woocommerce/"
    const val WOOCOMMERCE_WEB_OPTIONS = "https://woocommerce.com/tracking-and-opt-outs/"
    const val WOOCOMMERCE_USAGE_TRACKER = "https://woocommerce.com/usage-tracking/"

    const val URL_LEARN_MORE_REVIEWS = "https://woocommerce.com/document/ratings-and-reviews/"
    const val URL_LEARN_MORE_ORDERS = "https://woocommerce.com/blog/"

    const val WORDPRESS_PRIVACY_SETTINGS = "https://wordpress.com/support/settings/privacy-settings/"

    const val JETPACK_INSTRUCTIONS =
        "https://woocommerce.com/document/jetpack-setup-instructions-for-the-woocommerce-mobile-app//"
    const val JETPACK_TROUBLESHOOTING =
        "https://jetpack.com/support/getting-started-with-jetpack/troubleshooting-tips/"
    const val PRODUCT_IMAGE_UPLOADS_TROUBLESHOOTING =
        "https://woocommerce.com/document/troubleshooting-image-upload-issues-in-the-woo-mobile-apps/"
    const val ORDERS_TROUBLESHOOTING =
        "https://woocommerce.com/document/android-ios-apps-troubleshooting-error-fetching-orders/"

    const val CROWDSIGNAL_MAIN_SURVEY = "https://automattic.survey.fm/woo-app-general-feedback-user-survey"
    const val CROWDSIGNAL_PRODUCT_SURVEY = "https://automattic.survey.fm/woo-app-feature-feedback-products"
    const val CROWDSIGNAL_SHIPPING_LABELS_SURVEY =
        "https://automattic.survey.fm/woo-app-feature-feedback-shipping-labels"

    const val CROWDSIGNAL_ANALYTICS_HUB_SURVEY = "https://automattic.survey.fm/woo-app-analytics-hub-production"

    const val ORDER_CREATION_SURVEY = "https://automattic.survey.fm/woo-app-order-creation-production"

    const val ADDONS_SURVEY = "https://automattic.survey.fm/woo-app-addons-production"

    const val CROWDSIGNAL_STORE_SETUP_SURVEY =
        "https://automattic.survey.fm/woo-mobile-%E2%80%93-store-setup-survey-2022"

    const val CROWDSIGNAL_TAP_TO_PAY_SURVEY = "https://automattic.survey.fm/woo-app-%E2%80%93-first-ttp-survey"
    val CROWDSIGNAL_PRODCUT_CREATION_WITH_AI_SURVEY =
        if (BuildConfig.DEBUG) {
            "https://automattic.survey.fm/testing-debug-product-creation-with-ai-dec-2023"
        } else {
            "https://automattic.survey.fm/product-creation-with-ai-dec-2023"
        }

    val CROWDSIGNAL_ORDER_SHIPPING_LINES_SURVEY = if (BuildConfig.DEBUG) {
        "https://automattic.survey.fm/order-creation-shipping-lines-survey-testing"
    } else {
        "https://automattic.survey.fm/order-creation-shipping-lines-survey-production"
    }

    // Will be used later when the feature is fully launched.
    const val COUPONS_SURVEY = "https://automattic.survey.fm/woo-app-coupon-management-production"

    const val WOOCOMMERCE_USER_ROLES =
        "https://woocommerce.com/posts/a-guide-to-woocommerce-user-roles-permissions-and-security/"
    const val SHIPPING_LABEL_CUSTOMS_ITN = "https://pe.usps.com/text/imm/immc5_010.htm"
    const val SHIPPING_LABEL_CUSTOMS_HS_TARIFF_NUMBER =
        "https://woocommerce.com/document/woocommerce-shipping-and-tax/woocommerce-shipping/#section-29"

    const val WPCOM_ADD_PAYMENT_METHOD = "https://wordpress.com/me/purchases/add-payment-method"
    const val FETCH_PAYMENT_METHOD_URL_PATH = "me/payment-methods"
    const val WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS =
        "https://woocommerce.com/document/woopayments/in-person-payments/getting-started-with-in-person-payments/"
    const val STRIPE_LEARN_MORE_ABOUT_PAYMENTS =
        "https://woocommerce.com/document/stripe/accept-in-person-payments-with-stripe//"
    const val STRIPE_TAP_TO_PAY_DEVICE_REQUIREMENTS =
        "https://stripe.com/docs/terminal/payments/setup-reader/tap-to-pay?platform=android#supported-devices"
    const val LEARN_MORE_ABOUT_TAP_TO_PAY =
        "https://woocommerce.com/document/woopayments/in-person-payments/tap-to-pay-android/"

    const val WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY =
        "https://woocommerce.com/document/getting-started-with-in-person-payments-with-woocommerce-payments/" +
            "#add-cod-payment-method"
    const val STRIPE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY =
        "https://woocommerce.com/document/stripe/accept-in-person-payments-with-stripe/#section-8"

    const val WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY = "https://woocommerce.com/products/hardware/"

    const val BBPOS_MANUAL_CARD_READER =
        "https://woocommerce.com/wp-content/uploads/2022/12/c2xbt_product_sheet.pdf"
    const val M2_MANUAL_CARD_READER = "https://woocommerce.com/wp-content/uploads/2022/12/m2_product_sheet.pdf"
    const val WISEPAD_3_MANUAL_CARD_READER = "https://woocommerce.com/wp-content/uploads/2022/12/wp3_product_sheet.pdf"

    const val PLAY_STORE_APP_PREFIX = "http://play.google.com/store/apps/details?id="

    const val LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT =
        "https://woocommerce.com/document/what-is-a-wordpress-com-account/"

    const val NEW_TO_WOO_DOC = "https://woocommerce.com/woocommerce-features/"
    const val HOSTING_OPTIONS_DOC = "https://woocommerce.com/hosting-solutions/"

    const val WORPRESS_COM_TERMS = "https://wordpress.com/tos"
    const val JETPACK_SYNC_POLICY = "https://jetpack.com/support/what-data-does-jetpack-sync"

    private const val LOGIN_HELP_CENTER = "https://woocommerce.com/document/android-ios-apps-login-help-faq/"
    val LOGIN_HELP_CENTER_URLS = mapOf(
        LOGIN_EMAIL to "$LOGIN_HELP_CENTER#enter-wordpress-com-email-address-login-using-store-address-flow",
        LOGIN_SITE_ADDRESS to "$LOGIN_HELP_CENTER#enter-store-address",
        LOGIN_USERNAME_PASSWORD to "$LOGIN_HELP_CENTER#enter-store-credentials",
    )

    const val EU_SHIPPING_CUSTOMS_REQUIREMENTS = "https://www.usps.com/international/new-eu-customs-rules.htm"

    const val USPS_HAZMAT_INSTRUCTIONS = "https://www.uspsdelivers.com/hazmat-shipping-safety"
    const val USPS_HAZMAT_SEARCH_TOOL = "https://pe.usps.com/HAZMAT/Index"
    const val DHL_EXPRESS_HAZMAT_INSTRUCTIONS =
        "https://www.dhl.com/global-en/home/our-divisions/freight/customer-service/" +
            "dangerous-goods-and-prohibited-items.html"

    const val STORE_ONBOARDING_WCPAY_INSTRUCTIONS_WPCOM_ACCOUNT =
        "https://woocommerce.com/document/woopayments/startup-guide/#signing-up"
    const val STORE_ONBOARDING_WCPAY_INSTRUCTIONS_LEARN_MORE =
        "https://woocommerce.com/document/woopayments/our-policies/know-your-customer/"
    const val STORE_ONBOARDING_WCPAY_SETUP_GUIDE = "https://woocommerce.com/document/woopayments/startup-guide/"
    const val STORE_ONBOARDING_PAYMENTS_SETUP_GUIDE =
        "https://woocommerce.com/documentation/woocommerce/getting-started/sell-products/core-payment-options/"
    const val ADVERTISING_POLICY = "https://automattic.com/advertising-policy/"
    const val BLAZE_SUPPORT = "https://wordpress.com/support/promote-a-post/"

    fun getScreenshotUrl(themeDemoUrl: String) =
        "https://s0.wp.com/mshots/v1/$themeDemoUrl?demo=true/?w=1200&h=2400&vpw=400&vph=800"

    // Google for Woo
    const val GOOGLE_ADMIN_CAMPAIGN_CREATION_SUFFIX =
        "admin.php?page=wc-admin&path=%2Fgoogle%2Fdashboard&subpath=%2Fcampaigns%2Fcreate"

    const val GOOGLE_ADMIN_FIRST_CAMPAIGN_CREATION_SUCCESS_TRIGGER = "guide=campaign-creation-success"
    const val GOOGLE_ADMIN_SUBMISSION_SUCCESS_TRIGGER = "guide=submission-success"
    const val GOOGLE_ADMIN_SUBSEQUENT_CAMPAIGN_CREATION_SUCCESS_TRIGGER = "campaign=saved"

    const val GOOGLE_ADMIN_DASHBOARD =
        "admin.php?page=wc-admin&path=%2Fgoogle%2Fdashboard"
}
