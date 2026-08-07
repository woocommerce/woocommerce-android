package com.woocommerce.android.ui.login.auto

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.util.Locale
import javax.inject.Inject

internal class AutoLoginRequestParser @Inject constructor() {
    fun parse(payload: String): AutoLoginRequestParseResult {
        return try {
            JsonReader(StringReader(payload)).use { reader ->
                val request = readRequest(reader)
                if (reader.peek() != JsonToken.END_DOCUMENT) invalid()
                validate(request)
            }
        } catch (_: InvalidRequestException) {
            AutoLoginRequestParseResult.Invalid
        } catch (_: IllegalArgumentException) {
            AutoLoginRequestParseResult.Invalid
        } catch (_: IllegalStateException) {
            AutoLoginRequestParseResult.Invalid
        } catch (_: IOException) {
            AutoLoginRequestParseResult.Invalid
        }
    }

    private fun readRequest(reader: JsonReader): ParsedRequest {
        requireToken(reader, JsonToken.BEGIN_OBJECT)
        reader.beginObject()
        val seen = mutableSetOf<String>()
        var connection: String? = null
        var siteUrl: String? = null
        var username: String? = null
        var password: String? = null

        while (reader.hasNext()) {
            val name = reader.nextName()
            if (!seen.add(name)) invalid()
            when (name) {
                FIELD_CONNECTION -> connection = reader.nextBoundedString(MAX_CONNECTION_LENGTH)
                FIELD_SITE_URL -> siteUrl = reader.nextBoundedString(MAX_SITE_URL_LENGTH)
                FIELD_USERNAME -> username = reader.nextBoundedString(MAX_USERNAME_LENGTH)
                FIELD_PASSWORD -> password = reader.nextBoundedString(MAX_PASSWORD_LENGTH)
                else -> invalid()
            }
        }
        reader.endObject()
        return ParsedRequest(
            connection = connection ?: invalid(),
            siteUrl = siteUrl ?: invalid(),
            username = username ?: invalid(),
            password = password ?: invalid()
        )
    }

    private fun validate(parsed: ParsedRequest): AutoLoginRequestParseResult.Success {
        val connection = runCatching { AutoLoginConnection.valueOf(parsed.connection) }
            .getOrElse { invalid() }
        val siteUrl = parsed.siteUrl.trim()
        val username = parsed.username.trim()
        if (siteUrl.isEmpty() || username.isEmpty() || parsed.password.isEmpty()) invalid()

        val uri = runCatching { URI(siteUrl) }.getOrElse { invalid() }
        val scheme = uri.scheme?.lowercase(Locale.US) ?: invalid()
        if (scheme != HTTPS_SCHEME) invalid()
        if (!hasSafeSiteAuthorityAndPath(uri)) invalid()

        return AutoLoginRequestParseResult.Success(
            AutoLoginRequest(
                connection = connection,
                siteUrl = siteUrl,
                credentials = AutoLoginCredentials(username, parsed.password)
            )
        )
    }

    private fun hasSafeSiteAuthorityAndPath(uri: URI): Boolean =
        !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null

    private fun requireToken(reader: JsonReader, expected: JsonToken) {
        if (reader.peek() != expected) invalid()
    }

    private fun JsonReader.nextBoundedString(maxLength: Int): String {
        requireToken(this, JsonToken.STRING)
        return nextString().takeIf { it.length <= maxLength } ?: invalid()
    }

    private fun invalid(): Nothing = throw InvalidRequestException()

    private class InvalidRequestException : RuntimeException()

    private class ParsedRequest(
        val connection: String,
        val siteUrl: String,
        val username: String,
        val password: String
    ) {
        override fun toString(): String = "[REDACTED]"
    }

    companion object {
        private const val FIELD_CONNECTION = "connection"
        private const val FIELD_SITE_URL = "site_url"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_PASSWORD = "password"
        private const val HTTPS_SCHEME = "https"
        private const val MAX_CONNECTION_LENGTH = 16
        private const val MAX_SITE_URL_LENGTH = 2_048
        private const val MAX_USERNAME_LENGTH = 512
        private const val MAX_PASSWORD_LENGTH = 4_096
    }
}
