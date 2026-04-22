package com.woocommerce.android.ui.login.qrlogin

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import javax.inject.Inject

/**
 * Parses a raw QR payload string into a [QrLoginPayload].
 *
 * Expected JSON shapes (proposed, pending backend alignment):
 *  - `{ "v": 1, "type": "app_password", "url": "...", "password": "...", "username": "..." }`
 *  - `{ "v": 1, "type": "wpcom_token",  "token": "..." }`
 *  - `{ "v": 1, "type": "url_only",     "url": "..." }`
 *
 * Also accepts a bare URL string (e.g. `"https://store.example"`) as shorthand for `url_only`,
 * which keeps the fallback QR from woo.com simple to produce.
 *
 * Unknown / malformed / future-version payloads return [QrLoginPayload.Invalid].
 */
class QrLoginPayloadParser @Inject constructor(
    private val gson: Gson
) {
    fun parse(raw: String?): QrLoginPayload {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return QrLoginPayload.Invalid
        if (!trimmed.startsWith("{")) return parseBareUrl(trimmed)
        return parseJson(trimmed)
    }

    private fun parseBareUrl(raw: String): QrLoginPayload =
        if (looksLikeHttpUrl(raw)) QrLoginPayload.UrlOnly(siteUrl = raw) else QrLoginPayload.Invalid

    private fun parseJson(raw: String): QrLoginPayload {
        val json = runCatching { gson.fromJson(raw, JsonObject::class.java) }.getOrNull()
            ?: return QrLoginPayload.Invalid

        val version = json.get(KEY_VERSION)?.takeIf { it.isJsonPrimitive }?.asInt
        if (version != SUPPORTED_VERSION) return QrLoginPayload.Invalid

        return when (json.stringOrNull(KEY_TYPE)) {
            TYPE_APP_PASSWORD -> parseAppPassword(json)
            TYPE_WPCOM_TOKEN -> parseWpComToken(json)
            TYPE_URL_ONLY -> parseUrlOnly(json)
            else -> QrLoginPayload.Invalid
        }
    }

    private fun parseAppPassword(json: JsonObject): QrLoginPayload {
        val url = json.stringOrNull(KEY_URL)?.takeIf { looksLikeHttpUrl(it) } ?: return QrLoginPayload.Invalid
        val password = json.stringOrNull(KEY_PASSWORD)?.takeIf { it.isNotBlank() } ?: return QrLoginPayload.Invalid
        return QrLoginPayload.SiteAppPassword(
            siteUrl = url,
            username = json.stringOrNull(KEY_USERNAME),
            appPassword = password
        )
    }

    private fun parseWpComToken(json: JsonObject): QrLoginPayload {
        val token = json.stringOrNull(KEY_TOKEN)?.takeIf { it.isNotBlank() } ?: return QrLoginPayload.Invalid
        return QrLoginPayload.WpComToken(token)
    }

    private fun parseUrlOnly(json: JsonObject): QrLoginPayload {
        val url = json.stringOrNull(KEY_URL)?.takeIf { looksLikeHttpUrl(it) } ?: return QrLoginPayload.Invalid
        return QrLoginPayload.UrlOnly(url)
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    @Suppress("SwallowedException")
    private fun looksLikeHttpUrl(candidate: String): Boolean = try {
        val lower = candidate.lowercase()
        (lower.startsWith("http://") || lower.startsWith("https://")) && candidate.length > "https://".length
    } catch (_: JsonSyntaxException) {
        false
    }

    private companion object {
        const val SUPPORTED_VERSION = 1
        const val KEY_VERSION = "v"
        const val KEY_TYPE = "type"
        const val KEY_URL = "url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_TOKEN = "token"

        const val TYPE_APP_PASSWORD = "app_password"
        const val TYPE_WPCOM_TOKEN = "wpcom_token"
        const val TYPE_URL_ONLY = "url_only"
    }
}
