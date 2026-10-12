package org.wordpress.android.fluxc.network

import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader
import java.util.Locale

object UnexpectedStoreResponseClassifier {
    fun classify(statusCode: Int, contentType: String?, body: String): UnexpectedStoreResponseKind? {
        val trimmedBody = body.trim()
        val isRateLimited = statusCode == TOO_MANY_REQUESTS
        return when {
            statusCode in SERVER_ERROR_STATUS_CODES && !trimmedBody.isHandledWordPressError() ->
                UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE
            trimmedBody.isEmpty() && !isRateLimited -> null
            trimmedBody.isJson() -> null
            statusCode in SUCCESS_STATUS_CODES -> UnexpectedStoreResponseKind.UNEXPECTED_CONTENT
            isRateLimited || isHtml(contentType, trimmedBody) -> UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE
            else -> null
        }
    }

    private fun String.isJson(): Boolean = try {
        JsonReader(StringReader(this)).use { reader ->
            reader.strictness = Strictness.STRICT
            reader.skipValue()
            reader.peek() == JsonToken.END_DOCUMENT
        }
    } catch (_: IOException) {
        false
    }

    /**
     * A WordPress REST error with its own code, like a failed refund or app passwords being turned off, is handled
     * where the request is made. WordPress's critical error and a request stopped with wp_die still count.
     */
    private fun String.isHandledWordPressError(): Boolean {
        val error = takeIf { it.startsWith("{") }
            ?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
        val code = error?.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        return code != null && error?.has("message") == true && code !in UNEXPECTED_WORDPRESS_ERROR_CODES
    }

    private fun isHtml(contentType: String?, body: String): Boolean {
        val mediaType = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        return mediaType in HTML_MEDIA_TYPES || body.startsWith("<")
    }

    private const val TOO_MANY_REQUESTS = 429
    private val SUCCESS_STATUS_CODES = 200..299
    private val SERVER_ERROR_STATUS_CODES = 500..599
    private val HTML_MEDIA_TYPES = setOf("text/html", "application/xhtml+xml")
    private val UNEXPECTED_WORDPRESS_ERROR_CODES = setOf("internal_server_error", "wp_die")
}
