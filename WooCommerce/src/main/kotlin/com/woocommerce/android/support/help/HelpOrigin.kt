package com.woocommerce.android.support.help

enum class HelpOrigin(private val stringValue: String) {
    UNKNOWN("origin:unknown"),
    SETTINGS("origin:settings"),
    CARD_READER_ONBOARDING("origin:card_reader_onboarding"),
    CARD_READER_PAYMENT_ERROR("origin:card_reader_payment_error"),
    FEEDBACK_SURVEY("origin:feedback_survey"),
    USER_ELIGIBILITY_ERROR("origin:user_eligibility_error"),
    MY_STORE("origin:my_store"),
    ZENDESK_NOTIFICATION("origin:zendesk-notification"),
    LOGIN_EMAIL("origin:login-email"),
    LOGIN_MAGIC_LINK("origin:login-magic-link"),
    LOGIN_EMAIL_PASSWORD("origin:login-wpcom-password"),
    LOGIN_2FA("origin:login-2fa"),
    LOGIN_SITE_ADDRESS("origin:login-site-address"),
    LOGIN_SOCIAL("origin:login-social"),
    LOGIN_USERNAME_PASSWORD("origin:login-username-password"),
    LOGIN_EPILOGUE("origin:login-epilogue"),
    LOGIN_CONNECTED_EMAIL_HELP("origin:login-connected-email-help"),
    SIGNUP_EMAIL("origin:signup-email"),
    SIGNUP_MAGIC_LINK("origin:signup-magic-link"),
    JETPACK_INSTALLATION("origin:jetpack-installation"),
    JETPACK_ACTIVATION_USER_ELIGIBILITY_ERROR("origin:jetpack-activation-user-eligibility-error"),
    LOGIN_HELP_NOTIFICATION("origin:login-local-notification"),
    SITE_PICKER_JETPACK_TIMEOUT("origin:site-picker-jetpack-error"),
    LOGIN_WITH_QR_CODE("origin:qr-code-scanner"),
    STORE_CREATION("origin:store-creation"),
    DOMAIN_CHANGE("origin:domain-change"),
    UPGRADES("origin:upgrades"),
    ACCOUNT_DELETION("origin:account-deletion"),
    ORDERS_LIST("origin:orders-list"),
    BLAZE_CAMPAIGN_CREATION("origin:blaze-native-campaign-creation"),
    CONNECTIVITY_TOOL("origin:connectivity-tool"),
    APPLICATION_PASSWORD_TUTORIAL("origin:application-password-tutorial"),
    POS("origin:point-of-sale");

    override fun toString(): String {
        return stringValue
    }
}
