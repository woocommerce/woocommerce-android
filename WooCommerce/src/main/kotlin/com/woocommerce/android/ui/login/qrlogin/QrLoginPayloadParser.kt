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
 * Anything malformed, missing parameters, with the wrong scheme/host, or with a non-https
 * `siteUrl` returns [QrLoginPayload.Invalid]. The parser does not validate the token format
 * beyond non-blank — that's the server's job during exchange. `siteUrl` is parsed via OkHttp's
 * [okhttp3.HttpUrl] and rejected if it carries userinfo, query, or fragment components — those
 * have no role in a Woo site root and are classic spoofing surfaces in the confirmation prompt.
 */
class QrLoginPayloadParser @Inject constructor() {

    fun parse(raw: String?): QrLoginPayload {
        if (looksLikeInstallQr(raw)) return QrLoginPayload.InstallQrCode
        return parseTicket(raw) ?: QrLoginPayload.Invalid
    }

    private fun parseTicket(raw: String?): QrLoginPayload.Ticket? {
        val uri = parseDeepLink(raw) ?: return null
        val token = uri.queryParam(PARAM_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        val siteUrl = uri.queryParam(PARAM_SITE_URL)?.let(::normalizeSiteUrl) ?: return null
        return QrLoginPayload.Ticket(token = token, siteUrl = siteUrl)
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
        const val HTTPS_PREFIX = "https://"
        const val INSTALL_QR_HOST = "woocommerce.com"
        const val INSTALL_QR_PATH_FIRST_SEGMENT = "mobile"
    }
}
