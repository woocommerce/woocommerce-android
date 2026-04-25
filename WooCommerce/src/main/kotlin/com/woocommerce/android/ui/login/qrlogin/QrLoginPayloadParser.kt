package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.WooLog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        val params = extractQueryParams(raw) ?: return QrLoginPayload.Invalid
        val token = params[PARAM_TOKEN]?.takeIf { it.isNotBlank() }
        val siteUrl = params[PARAM_SITE_URL]?.let(::normalizeSiteUrl)
        return if (token != null && siteUrl != null) {
            QrLoginPayload.Ticket(token = token, siteUrl = siteUrl)
        } else {
            QrLoginPayload.Invalid
        }
    }

    private fun normalizeSiteUrl(raw: String): String? {
        if (raw.isBlank() || !raw.lowercase().startsWith(HTTPS_PREFIX)) return null
        val parsed = raw.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "https") return null
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            WooLog.w(WooLog.T.LOGIN, "QR login: rejecting siteUrl with userinfo")
            return null
        }
        if (parsed.querySize > 0 || parsed.fragment != null) {
            WooLog.w(WooLog.T.LOGIN, "QR login: rejecting siteUrl with query or fragment")
            return null
        }
        // Rebuild from the parsed URL so any normalization OkHttp applies (e.g. lowercased host)
        // is reflected in the value we hand to the exchange client and the confirmation dialog.
        return parsed.newBuilder().build().toString().trimEnd('/')
    }

    private fun extractQueryParams(raw: String?): Map<String, String>? {
        val trimmed = raw?.trim().orEmpty().takeIf { it.isNotEmpty() } ?: return null
        if (!trimmed.regionMatches(0, DEEP_LINK_PREFIX, 0, DEEP_LINK_PREFIX.length, ignoreCase = true)) return null
        val afterPrefix = trimmed.substring(DEEP_LINK_PREFIX.length).removePrefix("/")
        val query = afterPrefix.removePrefix("?").takeIf { afterPrefix.startsWith('?') && it.isNotBlank() }
            ?: return null
        return parseQuery(query)
    }

    private fun parseQuery(query: String): Map<String, String> = query.split('&')
        .mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0 || idx == pair.lastIndex) return@mapNotNull null
            val key = pair.substring(0, idx)
            val value = try {
                URLDecoder.decode(pair.substring(idx + 1), Charsets.UTF_8.name())
            } catch (e: IllegalArgumentException) {
                WooLog.w(WooLog.T.LOGIN, "QR login: failed to decode query param '$key': ${e.message}")
                return@mapNotNull null
            }
            key to value
        }
        .toMap()

    private companion object {
        const val DEEP_LINK_PREFIX = "woocommerce://qr-login"
        const val PARAM_TOKEN = "token"
        const val PARAM_SITE_URL = "siteUrl"
        const val HTTPS_PREFIX = "https://"
    }
}
