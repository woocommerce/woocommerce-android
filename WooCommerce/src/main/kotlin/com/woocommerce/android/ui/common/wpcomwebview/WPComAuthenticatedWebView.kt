package com.woocommerce.android.ui.common.wpcomwebview

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import org.wordpress.android.fluxc.network.UserAgent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WPComAuthenticatedWebView(
    url: String,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = (progress / 100f),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (progress == 100) 0f else 1f)
        )

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    this.webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let { onUrlLoaded(it) }
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            url?.let { onUrlLoaded(it) }
                        }
                    }
                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress == 100 || newProgress - progress >= 5) {
                                progress = newProgress
                            }
                        }
                    }
                    this.settings.javaScriptEnabled = true
                    this.settings.userAgentString = userAgent.userAgent
                }
            }
        ) { webView ->
            if (lastLoadedUrl == url) return@AndroidView
            lastLoadedUrl = url
            authenticator.authenticateAndLoadUrl(webView, url)
        }
    }
}
