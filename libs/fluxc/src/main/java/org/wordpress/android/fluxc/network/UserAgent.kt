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
    val apiUserAgent: String by lazy {
        val systemUserAgent = System.getProperty("http.agent") ?: ""
        "$systemUserAgent $appVersionName".trim()
    }

    /**
     * User-Agent string to be used in WebView.
     */
    val webViewUserAgent: String by lazy {
        val systemUserAgent = WebSettings.getDefaultUserAgent(appContext)
        "$systemUserAgent $appVersionName".trim()
    }

    override fun toString(): String = apiUserAgent
}
