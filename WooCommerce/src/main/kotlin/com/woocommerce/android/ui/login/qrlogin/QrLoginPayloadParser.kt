package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.WooLog
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Parses the deep link encoded in a login QR code:
 *
 * ```
 * woocommerce://qr-login?token=<64-byte hex>&siteUrl=<URL-encoded site URL>
 * ```
 *
 * Anything malformed, missing parameters, or with the wrong scheme/host returns
 * [QrLoginPayload.Invalid]. The parser does not validate the token format beyond non-blank —
 * that's the server's job during exchange.
 */
class QrLoginPayloadParser @Inject constructor() {

    fun parse(raw: String?): QrLoginPayload {
        val params = extractQueryParams(raw) ?: return QrLoginPayload.Invalid
        val token = params[PARAM_TOKEN]?.takeIf { it.isNotBlank() }
        val siteUrl = params[PARAM_SITE_URL]?.takeIf { it.isNotBlank() }
        return if (token != null && siteUrl != null) {
            QrLoginPayload.Ticket(token = token, siteUrl = siteUrl)
        } else {
            QrLoginPayload.Invalid
        }
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
    }
}
