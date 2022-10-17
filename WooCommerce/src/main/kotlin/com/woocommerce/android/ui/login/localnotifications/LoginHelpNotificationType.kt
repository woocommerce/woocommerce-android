package com.woocommerce.android.ui.login.localnotifications

enum class LoginHelpNotificationType(private val typeName: String) {
    LOGIN_SITE_ADDRESS_ERROR("site_address_error"),
    LOGIN_WPCOM_EMAIL_ERROR("wpcom_email_error"),
    LOGIN_SITE_ADDRESS_EMAIL_ERROR("site_address_email_error"),
    LOGIN_SITE_ADDRESS_PASSWORD_ERROR("site_address_wpcom_password_error"),
    LOGIN_WPCOM_PASSWORD_ERROR("wpcom_password_error"),
    DEFAULT_HELP("default_support");

    override fun toString(): String {
        return typeName
    }

    companion object {
        fun fromString(string: String?): LoginHelpNotificationType =
            when (string) {
                LOGIN_SITE_ADDRESS_ERROR.typeName -> LOGIN_SITE_ADDRESS_ERROR
                LOGIN_SITE_ADDRESS_EMAIL_ERROR.typeName -> LOGIN_SITE_ADDRESS_EMAIL_ERROR
                LOGIN_SITE_ADDRESS_PASSWORD_ERROR.typeName -> LOGIN_SITE_ADDRESS_PASSWORD_ERROR
                LOGIN_WPCOM_EMAIL_ERROR.typeName -> LOGIN_WPCOM_EMAIL_ERROR
                LOGIN_WPCOM_PASSWORD_ERROR.typeName -> LOGIN_WPCOM_PASSWORD_ERROR
                else -> DEFAULT_HELP
            }
    }
}
