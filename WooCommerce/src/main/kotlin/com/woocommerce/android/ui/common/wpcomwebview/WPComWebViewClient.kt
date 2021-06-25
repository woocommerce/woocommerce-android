package com.woocommerce.android.ui.common.wpcomwebview

import android.webkit.WebView
import android.webkit.WebViewClient

class WPComWebViewClient(private val urlInterceptor: UrlInterceptor) : WebViewClient() {
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        url?.let { urlInterceptor.onLoadUrl(url) }
    }
}
