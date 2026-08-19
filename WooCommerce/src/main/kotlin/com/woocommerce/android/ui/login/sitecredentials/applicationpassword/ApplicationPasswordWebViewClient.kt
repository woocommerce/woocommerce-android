package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.woocommerce.android.ui.compose.component.web.WCWebViewClient

internal class ApplicationPasswordWebViewClient(
    private val onMainFrameNavigationRequested: (String) -> Boolean
) : WCWebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (super.shouldOverrideUrlLoading(view, request)) return true

        return request
            ?.takeIf { it.isForMainFrame }
            ?.url
            ?.toString()
            ?.let(onMainFrameNavigationRequested)
            ?: false
    }
}
