package com.woocommerce.android.aiassistant.headless

import okio.ByteString.Companion.encodeUtf8

class WooAiSmokeRedactor(
    private val siteUrl: String,
    private val username: String,
    private val appPassword: String,
) {
    fun redact(value: String): String {
        val explicitSecrets = listOf(
            siteUrl,
            siteUrl.trimEnd('/'),
            username,
            appPassword,
            "$username:$appPassword".encodeUtf8().base64(),
        ).filter { it.isNotBlank() }

        return explicitSecrets
            .fold(value) { redacted, secret -> redacted.replace(secret, REDACTED) }
            .replace(BASIC_AUTH_PATTERN, "Basic $REDACTED")
            .replace(BEARER_AUTH_PATTERN, "Bearer $REDACTED")
            .replace(JWT_PATTERN, REDACTED)
    }

    companion object {
        private const val REDACTED = "[REDACTED]"
        private val BASIC_AUTH_PATTERN = Regex("Basic\\s+[A-Za-z0-9+/=._~-]+", RegexOption.IGNORE_CASE)
        private val BEARER_AUTH_PATTERN = Regex("Bearer\\s+[A-Za-z0-9+/=._~-]+", RegexOption.IGNORE_CASE)
        private val JWT_PATTERN = Regex(
            "[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{6,}"
        )
    }
}
