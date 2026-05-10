package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import okhttp3.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.min
import javax.inject.Inject

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
        )
    }

    private fun Response.firstAllowlistedRequestId(): String? =
        REQUEST_ID_HEADER_NAMES
            .firstNotNullOfOrNull { name -> header(name)?.takeIf { it.isNotBlank() } }

    companion object {
        private val REQUEST_ID_HEADER_NAMES = listOf(
            "X-Request-Id",
            "X-Request-ID",
            "X-WP-Request-ID",
            "X-WP-Request-Id",
        )
        const val MAX_RETRY_AFTER_MS = 5 * 60 * 1000L
        private const val RETRY_AFTER_HEADER = "Retry-After"
    }
}

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
            runCatching { Math.multiplyExact(seconds, 1_000L) }.getOrNull()
        }
        ?.takeIf { it > 0 }
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
