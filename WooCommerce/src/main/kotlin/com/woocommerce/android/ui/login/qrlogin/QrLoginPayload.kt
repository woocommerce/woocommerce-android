package com.woocommerce.android.ui.login.qrlogin

/**
 * Parsed representation of a scanned login QR code.
 *
 * The wire format is proposed (pending backend alignment):
 *
 * ```
 * { "v": 1, "type": "app_password", "url": "https://store.example", "password": "xxx" }
 * { "v": 1, "type": "wpcom_token", "token": "xxx" }
 * { "v": 1, "type": "url_only",    "url": "https://store.example" }
 * ```
 *
 * Any other shape is treated as [Invalid].
 */
sealed interface QrLoginPayload {
    data class SiteAppPassword(
        val siteUrl: String,
        val username: String?,
        val appPassword: String
    ) : QrLoginPayload

    data class WpComToken(val token: String) : QrLoginPayload

    data class UrlOnly(val siteUrl: String) : QrLoginPayload

    data object Invalid : QrLoginPayload
}
