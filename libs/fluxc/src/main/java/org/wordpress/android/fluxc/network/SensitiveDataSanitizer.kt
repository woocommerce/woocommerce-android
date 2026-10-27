package org.wordpress.android.fluxc.network

object SensitiveDataSanitizer {
    fun sanitize(value: String?): String {
        return value.orEmpty()
            // Removed first so a hidden character inside a key can't stop it from matching.
            .replace(INVISIBLE_CHARACTER_REGEX, "")
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
            .replace(COLON_SECRET_REGEX) { matchResult ->
                "${matchResult.groupValues[1]}[redacted]"
            }
            // PHP prints the arguments of each call in a stack trace, like the password passed to a login hook.
            .replace(STACK_TRACE_REGEX) { matchResult ->
                matchResult.value.replace(QUOTED_ARGUMENT_REGEX, "'[redacted]'")
            }
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private const val SECRET_KEYS =
        """consumer_key|application_password|password|pwd|\w*secret|\w*token|\w*nonce|api[_ -]?key"""
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val INVISIBLE_CHARACTER_REGEX = Regex("""[\p{Cf}\p{Cs}]|[\p{Cc}&&[^\r\n\t]]""")
    private val BEARER_TOKEN_REGEX = Regex("""(?i)\bBearer\s+[^\s,;]+""")
    private val COOKIE_HEADER_REGEX = Regex("""(?i)\b(Cookie|Set-Cookie):\s*[^\n\r,]+""")
    private val JSON_SECRET_REGEX = Regex(
        """("(?:$SECRET_KEYS)"\s*:\s*")[^"]*(")""",
        RegexOption.IGNORE_CASE
    )
    private val KEY_VALUE_SECRET_REGEX = Regex(
        """(?i)(^|[?&;\s])((?:$SECRET_KEYS)=)[^&;\s,"'}]+"""
    )
    private val COLON_SECRET_REGEX = Regex("""(?i)\b((?:$SECRET_KEYS)\s*:\s*)(?:"[^"]*"|\S+)""")
    private val STACK_TRACE_REGEX = Regex("""Stack trace:.*""", RegexOption.DOT_MATCHES_ALL)
    private val QUOTED_ARGUMENT_REGEX = Regex("""'(?:[^'\\]|\\.)*'""")
}
