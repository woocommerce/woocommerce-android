package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import org.wordpress.android.util.PackageUtils

class UserAgent(
    private val appContext: Context,
    private val appName: String
) {
    private val appVersionName by lazy {
        "$appName/${PackageUtils.getVersionName(appContext)}"
    }

    /**
     * User-Agent string when making API requests.
     */
    val userAgent: String by lazy {
        val systemUserAgent = System.getProperty("http.agent") ?: ""
        "$appVersionName $systemUserAgent".trim()
    }

    /**
     * User-Agent string to be used in WebView.
     */
    val webViewUserAgent: String by lazy {
        val systemUserAgent = WebSettings.getDefaultUserAgent(appContext)
        "$appVersionName $systemUserAgent".trim()
    }

    override fun toString(): String = userAgent
}
