package com.woocommerce.android.ui.login.qrlogin

/**
 * Result of parsing a scanned login QR code.
 *
 * The wire format is a deep link:
 *
 * ```
 * woocommerce://qr-login?token=<64-byte hex>&siteUrl=<URL-encoded site URL>
 * ```
 *
 * The [Ticket.token] is a single-use 5-minute bearer ticket — not a credential. The app exchanges
 * it for an Application Password by POSTing to
 * `{siteUrl}/wp-json/wc-admin/mobile-app/qr-login-exchange`.
 */
sealed interface QrLoginPayload {
    data class Ticket(
        val token: String,
        val siteUrl: String
    ) : QrLoginPayload

    /**
     * The merchant scanned the wp-admin onboarding QR that links to the App Store / Play Store
     * install pages (`https://woocommerce.com/mobile/?utm_source=wc_onboarding_mobile_task`).
     * The app is already installed, so the install QR is useless here — surfaced as a dedicated
     * error so we can explain the situation instead of falling back to "Not a WooCommerce code".
     */
    data object InstallQrCode : QrLoginPayload

    data object Invalid : QrLoginPayload
}
