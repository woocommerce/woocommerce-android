package com.woocommerce.android.ui.login.storesuggestion

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLDecoder
import javax.inject.Inject

class InstallReferrerParser @Inject constructor() {
    fun parse(referrer: String): ParsedInstallReferrer {
        val params = referrer.split('&')
            .mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                val key = parts[0].decodeOrNull() ?: return@mapNotNull null
                key to parts.getOrNull(1)?.decodeOrNull().orEmpty()
            }
            .toMap()
        val rawSiteUrl = params[PARAM_SITE_URL]?.trim()

        return ParsedInstallReferrer(
            campaign = params[PARAM_UTM_CAMPAIGN]?.takeIf { it.isNotBlank() },
            hasSiteUrl = !rawSiteUrl.isNullOrEmpty(),
            siteUrl = rawSiteUrl?.let(::normalizeSiteUrl),
        )
    }

    private fun normalizeSiteUrl(raw: String): String? {
        if (raw.isEmpty()) return null
        val withScheme = if (raw.contains("://")) raw else "https://$raw"
        val parsed = withScheme.toHttpUrlOrNull()
            ?.takeIf { it.scheme == "https" || it.scheme == "http" }
            ?: return null
        val hasUserInfo = parsed.username.isNotEmpty() || parsed.password.isNotEmpty()
        val hasQueryOrFragment = parsed.querySize > 0 || parsed.fragment != null
        if (hasUserInfo || hasQueryOrFragment || !parsed.host.contains('.')) return null
        return parsed.toString().trimEnd('/')
    }

    private fun String.decodeOrNull(): String? = try {
        URLDecoder.decode(this, Charsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val PARAM_UTM_CAMPAIGN = "utm_campaign"
        const val PARAM_SITE_URL = "site_url"
    }
}

data class ParsedInstallReferrer(
    val campaign: String?,
    val hasSiteUrl: Boolean,
    val siteUrl: String?,
)
