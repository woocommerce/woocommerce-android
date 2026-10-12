package org.wordpress.android.fluxc.network

internal object SensitiveDataSanitizer {
    fun sanitize(value: String?): String {
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
}
