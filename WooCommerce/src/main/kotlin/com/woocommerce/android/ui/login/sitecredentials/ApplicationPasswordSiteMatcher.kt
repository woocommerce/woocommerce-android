package com.woocommerce.android.ui.login.sitecredentials

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ApplicationPasswordSiteMatcher {
    private const val WWW_PREFIX = "www."

    fun isSameSite(siteAddress: String, candidateUrl: String?): Boolean {
        val siteHost = siteAddress.toSiteUrl()?.comparableHost() ?: return false
        val candidateHost = candidateUrl?.toSiteUrl()?.comparableHost() ?: return false
        return siteHost == candidateHost
    }

    private fun String.toSiteUrl(): HttpUrl? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return withScheme.toHttpUrlOrNull()
            ?.takeIf { it.username.isEmpty() && it.password.isEmpty() }
    }

    private fun HttpUrl.comparableHost(): String = host.lowercase().removePrefix(WWW_PREFIX)
}
