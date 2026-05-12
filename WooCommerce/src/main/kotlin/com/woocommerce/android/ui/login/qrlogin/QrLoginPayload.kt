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
 * The scanner also accepts the legacy wc-admin app-login QR in two shapes — self-hosted
 * credentials and WP.com email — mirroring what the OS-deeplink handler in
 * `LoginActivity.handleAppLoginUri` already accepts:
 *
 * ```
 * woocommerce://app-login?siteUrl=<URL-encoded site URL>&username=<URL-encoded username>
 * woocommerce://app-login?siteUrl=<URL-encoded site URL>&wpcomEmail=<URL-encoded email>
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

    /**
     * The merchant scanned the canonical wp.com magic-login URL
     * (`https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=…`).
     * The scanner hands [url] to the browser; wp.com then redirects to `woocommerce://magic-login`,
     * mirroring the path a 3rd-party scanner (Google Lens, etc.) takes today.
     */
    data class WpComMagicLinkUrl(val url: String) : QrLoginPayload

    /**
     * The merchant scanned a `woocommerce://qr-login?siteUrl=…` deeplink with no `token` (or a
     * blank one). The scanner routes them to the existing site-address login screen with the URL
     * prefilled and validation auto-started — bridging "scan QR" to "enter password / accept site"
     * with no manual typing.
     */
    data class SiteUrl(val siteUrl: String) : QrLoginPayload

    /**
     * Legacy wc-admin `app-login` QR. Two shapes are accepted, mirroring the OS-deeplink path:
     *  - [Credentials] for self-hosted sites: site URL + `username`, hands off to the existing
     *    site-credentials screen with both fields prefilled (password still required).
     *  - [WpComEmail] for WP.com-connected sites: site URL + `wpcomEmail`, hands off to the
     *    email/password screen via `gotWpcomSiteInfo`, matching the existing deeplink flow.
     *
     * Unlike [Ticket]/[SiteUrl], the site URL here may be http or https — these payloads do not
     * hit the QR-exchange endpoint, so we leave the scheme decision to the downstream login flow.
     */
    sealed interface AppLogin : QrLoginPayload {
        val siteUrl: String

        data class Credentials(
            override val siteUrl: String,
            val username: String
        ) : AppLogin

        data class WpComEmail(
            override val siteUrl: String,
            val wpComEmail: String
        ) : AppLogin
    }

    data object Invalid : QrLoginPayload
}
