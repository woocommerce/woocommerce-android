package org.wordpress.android.fluxc.network

import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import java.io.IOException
import java.io.StringReader
import java.util.Locale

object UnexpectedStoreResponseClassifier {
    fun classify(statusCode: Int, contentType: String?, body: String): UnexpectedStoreResponseKind? {
        val trimmedBody = body.trim()
        val isRateLimited = statusCode == TOO_MANY_REQUESTS
        return when {
            statusCode in SERVER_ERROR_STATUS_CODES && !trimmedBody.isApplicationPasswordsDisabledError() ->
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
     * WordPress sends these with a 501 when app passwords are turned off. The app already shows a screen for them.
     */
    private fun String.isApplicationPasswordsDisabledError(): Boolean = startsWith("{") &&
        runCatching { JsonParser.parseString(this).asJsonObject.get("code")?.asString }.getOrNull() in
        APPLICATION_PASSWORDS_DISABLED_ERROR_CODES

    private fun isHtml(contentType: String?, body: String): Boolean {
        val mediaType = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        return mediaType in HTML_MEDIA_TYPES || body.startsWith("<")
    }

    private const val TOO_MANY_REQUESTS = 429
    private val SUCCESS_STATUS_CODES = 200..299
    private val SERVER_ERROR_STATUS_CODES = 500..599
    private val HTML_MEDIA_TYPES = setOf("text/html", "application/xhtml+xml")
    private val APPLICATION_PASSWORDS_DISABLED_ERROR_CODES = setOf(
        ApplicationPasswordsManager.APPLICATION_PASSWORDS_DISABLED_ERROR_CODE,
        ApplicationPasswordsManager.APPLICATION_PASSWORDS_DISABLED_USER_ERROR_CODE
    )
}
