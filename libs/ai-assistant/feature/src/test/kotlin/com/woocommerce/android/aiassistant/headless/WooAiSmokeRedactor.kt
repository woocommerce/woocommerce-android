package com.woocommerce.android.aiassistant.headless

import okio.ByteString.Companion.encodeUtf8
import java.net.URI

class WooAiSmokeRedactor(
    private val siteUrl: String,
    private val wpComUsername: String,
    private val wpComPassword: String,
) {
    fun redact(value: String): String {
        val explicitSecrets = listOfNotNull(
            siteUrl,
            siteUrl.trimEnd('/'),
            siteUrl.hostOrNull(),
            wpComUsername,
            wpComPassword,
            "$wpComUsername:$wpComPassword".encodeUtf8().base64(),
        ).filter { it.isNotBlank() }

        return explicitSecrets
            .fold(value) { redacted, secret -> redacted.replace(secret, REDACTED) }
            .replace(BASIC_AUTH_PATTERN, "Basic $REDACTED")
            .replace(BEARER_AUTH_PATTERN, "Bearer $REDACTED")
            .replace(JWT_PATTERN, REDACTED)
            .replaceJsonPiiFields()
            .replace(EMAIL_PATTERN, REDACTED)
            .replace(PHONE_PATTERN, REDACTED)
    }

    companion object {
        private const val REDACTED = "[REDACTED]"
        private val PII_FIELD_NAMES = listOf(
            "first_name",
            "last_name",
            "name",
            "email",
            "phone",
            "address_1",
            "address_2",
            "city",
            "state",
            "postcode",
            "zip",
            "country",
            "company",
        ).joinToString("|")
        private val BASIC_AUTH_PATTERN = Regex("Basic\\s+[A-Za-z0-9+/=._~-]+", RegexOption.IGNORE_CASE)
        private val BEARER_AUTH_PATTERN = Regex("Bearer\\s+[A-Za-z0-9+/=._~-]+", RegexOption.IGNORE_CASE)
        private val JWT_PATTERN = Regex(
            "[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{6,}"
        )
        private val JSON_PII_FIELD_PATTERN = Regex(
            pattern = "(\"(?:billing_|shipping_)?(?:$PII_FIELD_NAMES)\"\\s*:\\s*)" +
                "(\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?)",
            option = RegexOption.IGNORE_CASE,
        )
        private val EMAIL_PATTERN = Regex(
            pattern = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
            option = RegexOption.IGNORE_CASE,
        )
        private val PHONE_PATTERN = Regex(
            "(?<!\\w)(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(\\d{2,4}\\)|\\d{2,4})" +
                "[\\s.-]\\d{3}[\\s.-]\\d{4}(?!\\w)"
        )

        private fun String.replaceJsonPiiFields(): String =
            JSON_PII_FIELD_PATTERN.replace(this) { matchResult ->
                "${matchResult.groupValues[1]}\"$REDACTED\""
            }
    }

    private fun String.hostOrNull(): String? =
        runCatching { URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }
}
