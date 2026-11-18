package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.ui.login.SiteHostRules
import java.net.URLDecoder
import javax.inject.Inject

class InstallReferrerParser @Inject constructor() {
    fun parse(referrer: String): ParsedInstallReferrer {
        val params = referrer.split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            val key = parts[0].decodeOrNull() ?: return@mapNotNull null
            key to parts.getOrNull(1)?.decodeOrNull()
        }
        val siteHostValues = params.filter { it.first == PARAM_SITE_HOST }.map { it.second }

        val siteHost = when {
            siteHostValues.isEmpty() -> SiteHost.Missing
            else -> siteHostValues.singleOrNull()?.let(SiteHostRules::normalize)
                ?.let { SiteHost.Valid(it) }
                ?: SiteHost.Invalid
        }

        return ParsedInstallReferrer(
            campaign = params.lastOrNull { it.first == PARAM_UTM_CAMPAIGN }?.second?.takeIf { it.isNotBlank() },
            siteHost = siteHost,
        )
    }

    private fun String.decodeOrNull(): String? = try {
        URLDecoder.decode(this, Charsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val PARAM_UTM_CAMPAIGN = "utm_campaign"
        const val PARAM_SITE_HOST = "site_host"
    }
}

data class ParsedInstallReferrer(
    val campaign: String?,
    val siteHost: SiteHost,
)

sealed interface SiteHost {
    data object Missing : SiteHost
    data object Invalid : SiteHost
    data class Valid(val host: String) : SiteHost
}
