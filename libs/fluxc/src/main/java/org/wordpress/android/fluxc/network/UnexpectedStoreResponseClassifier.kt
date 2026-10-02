package org.wordpress.android.fluxc.network

import java.util.Locale

object UnexpectedStoreResponseClassifier {
    fun classify(statusCode: Int, contentType: String?, body: String): UnexpectedStoreResponseKind? {
        val trimmedBody = body.trim()
        val isRateLimited = statusCode == TOO_MANY_REQUESTS
        return when {
            statusCode in SERVER_ERROR_STATUS_CODES -> UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE
            trimmedBody.isEmpty() && !isRateLimited -> null
            trimmedBody.isJson() -> null
            statusCode in SUCCESS_STATUS_CODES -> UnexpectedStoreResponseKind.UNEXPECTED_CONTENT
            isRateLimited || isHtml(contentType, trimmedBody) -> UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE
            else -> null
        }
    }

    private fun String.isJson(): Boolean =
        startsWith("{") || startsWith("[") || startsWith("\"") || matches(JSON_LITERAL_REGEX)

    private fun isHtml(contentType: String?, body: String): Boolean {
        val mediaType = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        return mediaType in HTML_MEDIA_TYPES || body.startsWith("<")
    }

    private const val TOO_MANY_REQUESTS = 429
    private val SUCCESS_STATUS_CODES = 200..299
    private val SERVER_ERROR_STATUS_CODES = 500..599
    private val HTML_MEDIA_TYPES = setOf("text/html", "application/xhtml+xml")
    private val JSON_LITERAL_REGEX = Regex("""true|false|null|-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?""")
}
