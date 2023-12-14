package com.woocommerce.android.ui.themes

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
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
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType
import org.wordpress.android.fluxc.network.UserAgent

@Suppress("ComplexMethod")
@Composable
fun ThemePreviewWebView(
    url: String,
    userAgent: UserAgent,
    modifier: Modifier = Modifier,
    wpComAuthenticator: WPComWebViewAuthenticator? = null,
    initialScale: Int,
    previewType: PreviewType
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }
    var lastLoadedUrl by remember { mutableStateOf("") }
    var lastPreviewType by remember { mutableStateOf(previewType) }

    Box(modifier = modifier) {
        fun getProgressAlpha(): Float {
            return if (progress == 100) 0f else 1f
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }

                    this.webViewClient = WebViewClient()
                    this.setInitialScale(initialScale)
                    this.settings.userAgentString = userAgent.userAgent
                }.also { webView = it }
            },
        ) { webView ->
            if (lastPreviewType != previewType) {
                lastPreviewType = previewType
                webView.setInitialScale(initialScale)
                wpComAuthenticator?.authenticateAndLoadUrl(webView, url) ?: webView.loadUrl(url)
            }
            if (lastLoadedUrl == url) return@AndroidView
            lastLoadedUrl = url
            wpComAuthenticator?.authenticateAndLoadUrl(webView, url) ?: webView.loadUrl(url)
        }

        LinearProgressIndicator(
            progress = (progress / 100f),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(getProgressAlpha())
        )
    }
}
