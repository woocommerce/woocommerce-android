package com.woocommerce.android.ui.common.wpcomwebview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

class WPComWebViewClient(private val urlIntercepter: UrlIntercepter) : WebViewClient() {
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        url?.let { urlIntercepter.onLoadUrl(url) }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }
}
