package com.woocommerce.android.ui.login

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/**
 * Accepts only a bare ASCII hostname that could belong to a public store. Anything else (scheme, user info, port,
 * path, IP literal, special-use name, punycode or non-ASCII) returns null, so the caller never shows a spoofable value.
 */
object SiteHostRules {
    private const val MAX_HOST_LENGTH = 253
    private val HOST_REGEX = Regex("^(?!-)[a-z0-9-]{1,63}(?<!-)(\\.(?!-)[a-z0-9-]{1,63}(?<!-))+$")
    private val TOP_LEVEL_LABEL_REGEX = Regex("^[a-z]{2,63}$")
    private val SPECIAL_USE_SUFFIXES = listOf(
        "localhost",
        "local",
        "internal",
        "test",
        "invalid",
        "example",
        "home.arpa",
    )
    private const val PUNYCODE_PREFIX = "xn--"

    fun normalize(raw: String): String? {
        if (raw.any { it.code > Byte.MAX_VALUE }) return null
        val host = raw.lowercase(Locale.ROOT).removeSuffix(".")
        val labels = host.split('.')
        val isValid = host.length <= MAX_HOST_LENGTH &&
            HOST_REGEX.matches(host) &&
            TOP_LEVEL_LABEL_REGEX.matches(labels.last()) &&
            labels.none { it.startsWith(PUNYCODE_PREFIX) } &&
            SPECIAL_USE_SUFFIXES.none { host == it || host.endsWith(".$it") } &&
            "https://$host".toHttpUrlOrNull()?.host == host
        return host.takeIf { isValid }
    }
}
