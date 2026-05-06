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
 * woocommerce://qr-login?token=<64–512 alphanumeric chars>&siteUrl=<URL-encoded site URL>
 * ```
 *
 * Anything malformed, missing parameters, with the wrong scheme/host, with a token that doesn't
 * match the expected shape, or with a non-https `siteUrl` returns [QrLoginPayload.Invalid].
 * The token shape mirrors the backend contract (currently `wp_generate_password(64, false)` →
 * 64 alphanumerics); the upper bound of 512 leaves headroom if the server lengthens the token
 * without forcing a client release in lockstep. The server is still the authority on whether a
 * given token is valid — this is just a sanity gate so obviously-malformed QRs don't advance
 * into the confirmation/exchange flow. `siteUrl` is parsed via OkHttp's [okhttp3.HttpUrl] and
 * rejected if it carries userinfo, query, or fragment components — those have no role in a Woo
 * site root and are classic spoofing surfaces in the confirmation prompt.
 */
class QrLoginPayloadParser @Inject constructor() {

    fun parse(raw: String?): QrLoginPayload {
        val uri = parseDeepLink(raw) ?: return QrLoginPayload.Invalid
        val token = uri.queryParam(PARAM_TOKEN)?.takeIf(TOKEN_REGEX::matches) ?: return QrLoginPayload.Invalid
        val siteUrl = uri.queryParam(PARAM_SITE_URL)?.let(::normalizeSiteUrl) ?: return QrLoginPayload.Invalid
        return QrLoginPayload.Ticket(token = token, siteUrl = siteUrl)
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
        val TOKEN_REGEX = Regex("^[A-Za-z0-9]{64,512}$")
    }
}
