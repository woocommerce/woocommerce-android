package com.woocommerce.android.ui.login.qrlogin

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Parses the deep link encoded in a login QR code:
 *
 * ```
 * woocommerce://qr-login?token=<64-byte hex>&siteUrl=<URL-encoded site URL>
 * ```
 *
 * The same deeplink shape is also used as a "site-URL only" QR — when `token` is missing or
 * blank, the payload becomes [QrLoginPayload.SiteUrl] and the scanner routes the merchant to
 * the site-address login screen with the URL prefilled instead of attempting an exchange.
 *
 * Anything malformed, missing parameters, with the wrong scheme/host, or with a non-https
 * `siteUrl` returns [QrLoginPayload.Invalid]. The parser does not validate the token format
 * beyond non-blank — that's the server's job during exchange. `siteUrl` is parsed via OkHttp's
 * [okhttp3.HttpUrl] and rejected if it carries userinfo, query, or fragment components — those
 * have no role in a Woo site root and are classic spoofing surfaces in the confirmation prompt.
 *
 * Also recognises the WordPress.com magic-login QR
 * (`https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=…`) and
 * surfaces it as [QrLoginPayload.WpComMagicLinkUrl] for the scanner to hand off via `ACTION_VIEW`.
 */
class QrLoginPayloadParser @Inject constructor() {

    fun parse(raw: String?): QrLoginPayload {
        parseWpComMagicLinkUrl(raw)?.let { return it }
        if (looksLikeInstallQr(raw)) return QrLoginPayload.InstallQrCode
        return parseQrLoginDeeplink(raw) ?: QrLoginPayload.Invalid
    }

    /**
     * Validates the wp.com magic-login URL and returns it verbatim. The `scheme=woocommerce`
     * requirement is load-bearing: a QR with `scheme=wordpress` is intended for the WordPress
     * app and we must not silently launch its URL here.
     */
    private fun parseWpComMagicLinkUrl(raw: String?): QrLoginPayload.WpComMagicLinkUrl? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        val matches = parsed.scheme == "https" &&
            parsed.host.equals(WP_COM_HOST, ignoreCase = true) &&
            parsed.encodedPath == WP_COM_MAGIC_LINK_PATH &&
            parsed.queryParameter(PARAM_ACTION)?.lowercase() == ACTION_MAGIC_LOGIN &&
            parsed.queryParameter(PARAM_SCHEME)?.lowercase() == SCHEME &&
            !parsed.queryParameter(PARAM_TOKEN).isNullOrBlank()
        return if (matches) QrLoginPayload.WpComMagicLinkUrl(url = trimmed) else null
    }

    /**
     * Parses the `woocommerce://qr-login?...` deeplink. Returns [QrLoginPayload.Ticket] when both
     * `token` and `siteUrl` are present and valid, [QrLoginPayload.SiteUrl] when `siteUrl` is
     * valid but `token` is missing or blank, and `null` (caller maps to `Invalid`) otherwise.
     * `siteUrl` validation is identical in both branches.
     */
    private fun parseQrLoginDeeplink(raw: String?): QrLoginPayload? {
        val uri = parseDeepLink(raw) ?: return null
        val siteUrl = uri.queryParam(PARAM_SITE_URL)?.let(::normalizeSiteUrl) ?: return null
        val token = uri.queryParam(PARAM_TOKEN)?.takeIf { it.isNotBlank() }
        return if (token != null) {
            QrLoginPayload.Ticket(token = token, siteUrl = siteUrl)
        } else {
            QrLoginPayload.SiteUrl(siteUrl = siteUrl)
        }
    }

    /**
     * The wp-admin onboarding flow renders a QR that points to
     * `https://woocommerce.com/mobile/...` — its purpose is to land users on the App Store /
     * Play Store install pages. If we see one of those URLs we know the merchant scanned the
     * install QR rather than the sign-in QR; the app is already installed, so we can surface
     * a helpful explanation instead of "Not a WooCommerce code".
     */
    private fun looksLikeInstallQr(raw: String?): Boolean {
        val parsed = raw?.trim()?.takeIf { it.isNotEmpty() }?.toHttpUrlOrNull() ?: return false
        if (parsed.scheme != "https") return false
        if (!parsed.host.equals(INSTALL_QR_HOST, ignoreCase = true)) return false
        val pathSegments = parsed.encodedPathSegments.filter { it.isNotEmpty() }
        return pathSegments.firstOrNull()?.equals(INSTALL_QR_PATH_FIRST_SEGMENT, ignoreCase = true) == true
    }

    private fun parseDeepLink(raw: String?): URI? {
        val trimmed = raw?.trim().orEmpty().takeIf { it.isNotEmpty() } ?: return null
        val uri = try {
            URI(trimmed)
        } catch (_: URISyntaxException) {
            return null
        }
        val schemeMatches = uri.scheme.equals(SCHEME, ignoreCase = true)
        val hostMatches = uri.host?.equals(HOST, ignoreCase = true) == true
        val pathAllowed = uri.rawPath.isNullOrEmpty() || uri.rawPath == "/"
        return uri.takeIf { schemeMatches && hostMatches && pathAllowed }
    }

    private fun URI.queryParam(name: String): String? {
        val query = rawQuery ?: return null
        val rawValue = query.split('&').firstNotNullOfOrNull { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0 && pair.substring(0, idx) == name) pair.substring(idx + 1) else null
        } ?: return null
        return try {
            URLDecoder.decode(rawValue, Charsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun normalizeSiteUrl(raw: String): String? {
        if (raw.isBlank() || !raw.lowercase().startsWith(HTTPS_PREFIX)) return null
        val parsed = raw.toHttpUrlOrNull()?.takeIf { it.scheme == "https" } ?: return null
        val hasUserInfo = parsed.username.isNotEmpty() || parsed.password.isNotEmpty()
        val hasQueryOrFragment = parsed.querySize > 0 || parsed.fragment != null
        if (hasUserInfo || hasQueryOrFragment) return null
        // Rebuild from the parsed URL so any normalization OkHttp applies (e.g. lowercased host)
        // is reflected in the value we hand to the exchange client and the confirmation dialog.
        return parsed.newBuilder().build().toString().trimEnd('/')
    }

    private companion object {
        const val SCHEME = "woocommerce"
        const val HOST = "qr-login"
        const val PARAM_TOKEN = "token"
        const val PARAM_SITE_URL = "siteUrl"
        const val PARAM_ACTION = "action"
        const val PARAM_SCHEME = "scheme"
        const val HTTPS_PREFIX = "https://"
        const val INSTALL_QR_HOST = "woocommerce.com"
        const val INSTALL_QR_PATH_FIRST_SEGMENT = "mobile"
        const val WP_COM_HOST = "wordpress.com"
        const val WP_COM_MAGIC_LINK_PATH = "/wp-login.php"
        const val ACTION_MAGIC_LOGIN = "magic-login"
    }
}
