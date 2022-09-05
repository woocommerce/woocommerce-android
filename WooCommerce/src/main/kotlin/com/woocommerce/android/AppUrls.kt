package com.woocommerce.android

import com.woocommerce.android.support.HelpActivity.Origin.LOGIN_EMAIL
import com.woocommerce.android.support.HelpActivity.Origin.LOGIN_SITE_ADDRESS
import com.woocommerce.android.support.HelpActivity.Origin.LOGIN_USERNAME_PASSWORD

object AppUrls {
    const val APP_HELP_CENTER = "https://docs.woocommerce.com/document/android/"
    const val APP_FEATURE_REQUEST = "http://ideas.woocommerce.com/forums/133476-woocommerce?category_id=84283"

    const val AUTOMATTIC_HOME = "https://www.automattic.com/"
    const val AUTOMATTIC_TOS = "https://woocommerce.com/terms-conditions/"
    const val AUTOMATTIC_PRIVACY_POLICY = "https://www.automattic.com/privacy"
    const val AUTOMATTIC_PRIVACY_POLICY_CA = "https://automattic.com/privacy/#california-consumer-privacy-act-ccpa"
    const val AUTOMATTIC_COOKIE_POLICY = "https://www.automattic.com/cookies"
    const val AUTOMATTIC_HIRING = "https://automattic.com/work-with-us"

    const val WOOCOMMERCE_UPGRADE = "https://docs.woocommerce.com/document/how-to-update-woocommerce/"
    const val WOOCOMMERCE_PLUGIN = "https://wordpress.org/plugins/woocommerce/"

    const val URL_LEARN_MORE_REVIEWS = "https://woocommerce.com/posts/reviews-woocommerce-best-practices/"
    const val URL_LEARN_MORE_ORDERS = "https://woocommerce.com/blog/"

    const val JETPACK_INSTRUCTIONS =
        "https://docs.woocommerce.com/document/jetpack-setup-instructions-for-the-woocommerce-mobile-app/"
    const val JETPACK_TROUBLESHOOTING =
        "https://jetpack.com/support/getting-started-with-jetpack/troubleshooting-tips/"
    const val PRODUCT_IMAGE_UPLOADS_TROUBLESHOOTING =
        "https://docs.woocommerce.com/document/troubleshooting-image-upload-issues-in-the-woo-mobile-apps/"

    const val CROWDSIGNAL_MAIN_SURVEY = "https://automattic.survey.fm/woo-app-general-feedback-user-survey"
    const val CROWDSIGNAL_PRODUCT_SURVEY = "https://automattic.survey.fm/woo-app-feature-feedback-products"
    const val CROWDSIGNAL_SHIPPING_LABELS_SURVEY =
        "https://automattic.survey.fm/woo-app-feature-feedback-shipping-labels"

    const val SIMPLE_PAYMENTS_SURVEY = "https://automattic.survey.fm/woo-app-quick-order-production"

    const val ORDER_CREATION_SURVEY = "https://automattic.survey.fm/woo-app-order-creation-production"

    const val ADDONS_SURVEY = "https://automattic.survey.fm/woo-app-addons-production"

    const val COUPONS_SURVEY_DEBUG = "https://automattic.survey.fm/woo-app-coupon-management-testing"

    // Will be used later when the feature is fully launched.
    const val COUPONS_SURVEY = "https://automattic.survey.fm/woo-app-coupon-management-production"

    const val WOOCOMMERCE_USER_ROLES =
        "https://woocommerce.com/posts/a-guide-to-woocommerce-user-roles-permissions-and-security/"
    const val SHIPPING_LABEL_CUSTOMS_ITN = "https://pe.usps.com/text/imm/immc5_010.htm"
    const val SHIPPING_LABEL_CUSTOMS_HS_TARIFF_NUMBER =
        "https://docs.woocommerce.com/document/woocommerce-shipping-and-tax/woocommerce-shipping/#section-29"

    const val WPCOM_ADD_PAYMENT_METHOD = "https://wordpress.com/me/purchases/add-payment-method"
    const val WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS =
        "https://docs.woocommerce.com/document/getting-started-with-in-person-payments-with-woocommerce-payments/"
    const val STRIPE_LEARN_MORE_ABOUT_PAYMENTS =
        "https://docs.woocommerce.com/document/stripe/accept-in-person-payments-with-stripe/"

    const val WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY =
        "https://docs.woocommerce.com/document/getting-started-with-in-person-payments-with-woocommerce-payments/" +
            "#add-cod-payment-method"
    const val STRIPE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY =
        "https://docs.woocommerce.com/document/stripe/accept-in-person-payments-with-stripe/#section-8"

    const val WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY = "https://woocommerce.com/products/hardware/"

    const val BBPOS_MANUAL_CARD_READER =
        "https://stripe.com/files/docs/terminal/c2xbt_product_sheet.pdf"
    const val M2_MANUAL_CARD_READER = "https://stripe.com/files/docs/terminal/m2_product_sheet.pdf"
    const val WISEPAD_3_MANUAL_CARD_READER = "https://stripe.com/files/docs/terminal/wp3_product_sheet.pdf"

    const val PLAY_STORE_APP_PREFIX = "http://play.google.com/store/apps/details?id="

    const val PLUGIN_MANAGEMENT_SUFFIX = "/wp-admin/plugins.php"

    const val LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT =
        "https://woocommerce.com/document/what-is-a-wordpress-com-account/"

    const val NEW_TO_WOO_DOC = "https://woocommerce.com/document/woocommerce-features"

    private const val LOGIN_HELP_CENTER = "https://woocommerce.com/document/android-ios-apps-login-help-faq/"
    val LOGIN_HELP_CENTER_URLS = mapOf(
        LOGIN_EMAIL to "$LOGIN_HELP_CENTER#enter-wordpress-com-email-address-login-using-store-address-flow",
        LOGIN_SITE_ADDRESS to "$LOGIN_HELP_CENTER#enter-store-address",
        LOGIN_USERNAME_PASSWORD to "$LOGIN_HELP_CENTER#enter-store-credentials",
    )
}
