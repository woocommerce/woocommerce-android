package com.woocommerce.android.ui.login.storesuggestion

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object SuggestedHostMatcher {
    private const val WWW_PREFIX = "www."

    fun isSameHost(submittedUrl: String, finalUrl: String): Boolean {
        val submittedHost = submittedUrl.hostOrNull() ?: return false
        val finalHost = finalUrl.hostOrNull() ?: return false
        return submittedHost.removePrefix(WWW_PREFIX) == finalHost.removePrefix(WWW_PREFIX)
    }

    private fun String.hostOrNull(): String? =
        (if (contains("://")) this else "https://$this").toHttpUrlOrNull()?.host
}
