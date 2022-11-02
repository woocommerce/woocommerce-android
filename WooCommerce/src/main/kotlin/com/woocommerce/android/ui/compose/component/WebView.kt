package com.woocommerce.android.ui.compose.component

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import org.wordpress.android.fluxc.network.UserAgent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WCWebView(
    url: String,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    wpComAuthenticator: WPComWebViewAuthenticator? = null,
    webViewNavigator: WebViewNavigator = rememberWebViewNavigator()
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }
    var lastLoadedUrl by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(captureBackPresses && canGoBack) {
        webView?.goBack()
    }

    LaunchedEffect(webView, webViewNavigator) {
        with(webViewNavigator) {
            webView?.handleNavigationEvents()
        }
    }

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
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let { onUrlLoaded(it) }
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            url?.let { onUrlLoaded(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() ?: false
                        }
                    }
                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    this.settings.javaScriptEnabled = true
                    this.settings.domStorageEnabled = true
                    this.settings.userAgentString = userAgent.userAgent
                }.also { webView = it }
            }
        ) { webView ->
            if (lastLoadedUrl == url) return@AndroidView
            lastLoadedUrl = url
            wpComAuthenticator?.authenticateAndLoadUrl(webView, url) ?: webView.loadUrl(url)
            canGoBack = webView.canGoBack()
        }
    }
}

@Stable
class WebViewNavigator {
    private enum class NavigationEvent {
        BACK, FORWARD
    }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow(extraBufferCapacity = 1)
    private var webView: WebView? = null

    suspend fun WebView.handleNavigationEvents() {
        webView = this
        navigationEvents
            .onCompletion {
                webView = null
            }
            .collect {
                when (it) {
                    NavigationEvent.BACK -> goBack()
                    NavigationEvent.FORWARD -> goForward()
                }
            }
    }

    val canGoBack
        get() = webView?.canGoBack() ?: false

    val canGoForward
        get() = webView?.canGoForward() ?: false

    fun navigateBack() {
        navigationEvents.tryEmit(NavigationEvent.BACK)
    }

    fun navigateForward() {
        navigationEvents.tryEmit(NavigationEvent.FORWARD)
    }
}

@Composable
fun rememberWebViewNavigator(): WebViewNavigator = remember { WebViewNavigator() }
