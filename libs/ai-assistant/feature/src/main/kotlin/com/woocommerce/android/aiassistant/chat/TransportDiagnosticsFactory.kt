package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import okhttp3.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.min

private const val MILLIS_PER_SECOND = 1_000L

internal class TransportDiagnosticsFactory @Inject constructor() {
    fun from(
        response: Response?,
        nowMillis: Long = System.currentTimeMillis(),
    ): TransportDiagnostics? {
        response ?: return null
        return TransportDiagnostics(
            httpStatus = response.code,
            requestId = response.firstAllowlistedRequestId(),
            retryAfterMs = response.header(RETRY_AFTER_HEADER)?.parseRetryAfter(nowMillis),
            bodySnippet = response.safeBodySnippet(),
        )
    }

    fun fromRawHttp(
        statusCode: Int?,
        headers: Map<String, String>? = null,
        bodyBytes: ByteArray? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): TransportDiagnostics? {
        val normalizedHeaders = headers.orEmpty()
        val diagnostics = TransportDiagnostics(
            httpStatus = statusCode,
            requestId = normalizedHeaders.firstAllowlistedRequestId(),
            retryAfterMs = normalizedHeaders.header(RETRY_AFTER_HEADER)?.parseRetryAfter(nowMillis),
            bodySnippet = bodyBytes.safeBodySnippet(),
        )
        return diagnostics.takeIf {
            it.httpStatus != null ||
                it.requestId != null ||
                it.retryAfterMs != null ||
                it.bodySnippet != null
        }
    }

    private fun Response.firstAllowlistedRequestId(): String? =
        REQUEST_ID_HEADER_NAMES
            .firstNotNullOfOrNull { name -> header(name)?.takeIf { it.isNotBlank() } }

    companion object {
        internal val REQUEST_ID_HEADER_NAMES = listOf(
            "X-Request-Id",
            "X-WP-Request-Id",
        )
        const val MAX_RETRY_AFTER_MS = 5 * 60 * 1000L
        const val MAX_BODY_SNIPPET_BYTES = 4096L
        const val MAX_BODY_SNIPPET_CHARS = 2048
        private const val RETRY_AFTER_HEADER = "Retry-After"
        val SENSITIVE_HEADER_PATTERN = Regex(
            pattern = """(?im)\b(Authorization|Cookie|Set-Cookie)\s*:\s*[^\r\n]+""",
        )
        val SENSITIVE_JSON_VALUE_PATTERN = Regex(
            pattern = """(?i)(["'](?:Authorization|Cookie|Set-Cookie)["']\s*:\s*)(["'])(?:\\.|(?!\2).)*\2""",
        )
        val SENSITIVE_KEY_VALUE_PATTERN = Regex(
            pattern = """(?im)\b(Authorization|Cookie|Set-Cookie)\s*=\s*[^\r\n&]+""",
        )
        val BEARER_TOKEN_PATTERN = Regex(
            pattern = """(?i)\bBearer\s+[A-Za-z0-9._~+/\-=]+""",
        )
    }
}

private fun Map<String, String>.firstAllowlistedRequestId(): String? =
    TransportDiagnosticsFactory.REQUEST_ID_HEADER_NAMES
        .firstNotNullOfOrNull { name -> header(name)?.takeIf { it.isNotBlank() } }

private fun Map<String, String>.header(name: String): String? =
    entries.firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }?.value

private fun Response.safeBodySnippet(): String? =
    runCatching {
        peekBody(TransportDiagnosticsFactory.MAX_BODY_SNIPPET_BYTES)
            .string()
            .takeIf { it.isNotBlank() }
            ?.redactSensitiveValues()
            ?.take(TransportDiagnosticsFactory.MAX_BODY_SNIPPET_CHARS)
    }.getOrNull()

private fun ByteArray?.safeBodySnippet(): String? =
    this
        ?.take(TransportDiagnosticsFactory.MAX_BODY_SNIPPET_BYTES.toInt())
        ?.toByteArray()
        ?.decodeToString()
        ?.takeIf { it.isNotBlank() }
        ?.redactSensitiveValues()
        ?.take(TransportDiagnosticsFactory.MAX_BODY_SNIPPET_CHARS)

private fun String.redactSensitiveValues(): String =
    replace(TransportDiagnosticsFactory.SENSITIVE_JSON_VALUE_PATTERN) { match ->
        "${match.groupValues[1]}${match.groupValues[2]}[REDACTED]${match.groupValues[2]}"
    }.replace(TransportDiagnosticsFactory.SENSITIVE_HEADER_PATTERN) { match ->
        "${match.groupValues[1]}: [REDACTED]"
    }.replace(TransportDiagnosticsFactory.SENSITIVE_KEY_VALUE_PATTERN) { match ->
        "${match.groupValues[1]}=[REDACTED]"
    }.replace(TransportDiagnosticsFactory.BEARER_TOKEN_PATTERN, "Bearer [REDACTED]")

private fun String.parseRetryAfter(nowMillis: Long): Long? =
    trim()
        .takeIf { it.isNotEmpty() }
        ?.let { value ->
            value.parseRetryAfterSeconds()
                ?: value.parseRetryAfterDate(nowMillis)
        }

private fun String.parseRetryAfterSeconds(): Long? =
    toLongOrNull()
        ?.takeIf { it >= 0 }
        ?.let { seconds ->
            runCatching { Math.multiplyExact(seconds, MILLIS_PER_SECOND) }.getOrNull()
        }
        ?.let { min(it, TransportDiagnosticsFactory.MAX_RETRY_AFTER_MS) }

private fun String.parseRetryAfterDate(nowMillis: Long): Long? =
    try {
        ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli()
            .minus(nowMillis)
            .takeIf { it > 0 }
            ?.let { min(it, TransportDiagnosticsFactory.MAX_RETRY_AFTER_MS) }
    } catch (_: DateTimeParseException) {
        null
    }
