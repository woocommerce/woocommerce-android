package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.util.AppLog

object JetpackTunnelRawBodyErrorLogger {
    fun buildMessage(method: String, path: String, error: WPComGsonNetworkError): String? {
        val rawBody = error.errorData?.optString("raw_body")?.takeIf { it.isNotBlank() } ?: return null
        val sanitizedRawBody = sanitize(rawBody)
        val isRawBodyTruncated = sanitizedRawBody.length > MAX_RAW_BODY_LOG_CHARS
        val rawBodySnippet = sanitizedRawBody.take(MAX_RAW_BODY_LOG_CHARS)
        val fields = listOfNotNull(
            "method=${sanitize(method)}",
            "path=${sanitize(path)}",
            error.volleyError?.networkResponse?.statusCode?.let { "transport_status=$it" },
            error.errorData?.opt("status")?.let { "proxy_status=${sanitize(it.toString())}" },
            "error_code=${sanitize(error.apiError)}",
            "error_message=${sanitize(error.message)}",
            "raw_body_truncated=$isRawBodyTruncated",
            "raw_body_snippet=$rawBodySnippet"
        )
        return "Jetpack Tunnel raw_body error: ${fields.joinToString(", ")}"
    }

    fun logIfPresent(method: String, path: String, error: WPComGsonNetworkError) {
        buildMessage(method, path, error)?.let { message ->
            AppLog.w(AppLog.T.API, message)
        }
    }

    private fun sanitize(value: String?): String {
        return value.orEmpty()
            .replace(BEARER_TOKEN_REGEX, "Bearer [redacted]")
            .replace(COOKIE_HEADER_REGEX) { matchResult ->
                "${matchResult.groupValues[1]}: [redacted]"
            }
            .replace(JSON_SECRET_REGEX) { matchResult ->
                "${matchResult.groupValues[1]}[redacted]${matchResult.groupValues[2]}"
            }
            .replace(KEY_VALUE_SECRET_REGEX) { matchResult ->
                "${matchResult.groupValues[1]}${matchResult.groupValues[2]}[redacted]"
            }
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val BEARER_TOKEN_REGEX = Regex("""(?i)\bBearer\s+[^\s,;]+""")
    private val COOKIE_HEADER_REGEX = Regex("""(?i)\b(Cookie|Set-Cookie):\s*[^\n\r,]+""")
    private val JSON_SECRET_REGEX = Regex(
        """("(?:consumer_key|consumer_secret|access_token|token|application_password|password)"\s*:\s*")[^"]*(")""",
        RegexOption.IGNORE_CASE
    )
    private val KEY_VALUE_SECRET_REGEX = Regex(
        """(?i)(^|[?&;\s])((?:consumer_key|consumer_secret|access_token|token|application_password|password)=)[^&;\s,"'}]+"""
    )
    private const val MAX_RAW_BODY_LOG_CHARS = 2048
}
