package com.woocommerce.android.ui.compose.component.web

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.web.WebViewProgressIndicator.Circular
import com.woocommerce.android.ui.compose.component.web.WebViewProgressIndicator.Linear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import org.wordpress.android.fluxc.network.UserAgent

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Suppress("ComplexMethod")
@Composable
fun WCWebView(
    url: String,
    userAgent: UserAgent,
    modifier: Modifier = Modifier,
    onUrlLoaded: (String) -> Unit = {},
    onPageFinished: (String) -> Unit = {},
    onUrlFailed: (String, Int?) -> Unit = { _, _ -> },
    captureBackPresses: Boolean = true,
    wpComAuthenticator: WPComWebViewAuthenticator? = null,
    webViewNavigator: WebViewNavigator = rememberWebViewNavigator(),
    activityRegistry: ActivityResultRegistry? = null,
    loadWithOverviewMode: Boolean = false,
    useWideViewPort: Boolean = false,
    isJavaScriptEnabled: Boolean = true,
    isDomStorageEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    initialScale: Int = 0,
    clearCache: Boolean = false,
    progressIndicator: WebViewProgressIndicator = Linear()
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

    Box(modifier = modifier) {
        fun getWebViewAlpha(): Float {
            return if (progressIndicator is Circular ||
                progressIndicator is Linear && progressIndicator.message != null
            ) {
                if (progress == 100) 1f else 0f
            } else {
                1f
            }
        }

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

                    this.webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let { onUrlLoaded(it) }
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            url?.let { onUrlLoaded(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            url?.let { onPageFinished(it) }
                            canGoBack = view?.canGoBack() ?: false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            request?.url?.let { url ->
                                onUrlFailed(url.toString(), error?.errorCode)
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            request?.url?.let { url ->
                                onUrlFailed(url.toString(), errorResponse?.statusCode)
                            }
                        }
                    }

                    if (activityRegistry != null) {
                        this.webChromeClient =
                            WebChromeClientWithImageChooser(activityRegistry) { newProgress -> progress = newProgress }
                    } else {
                        this.webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                    }

                    if (isReadOnly) {
                        this.setOnTouchListener { _, _ -> true }
                    }

                    this.setInitialScale(initialScale)
                    this.settings.useWideViewPort = useWideViewPort
                    this.settings.loadWithOverviewMode = loadWithOverviewMode
                    this.settings.javaScriptEnabled = isJavaScriptEnabled
                    this.settings.domStorageEnabled = isDomStorageEnabled
                    this.settings.userAgentString = userAgent.userAgent
                    if (clearCache) {
                        WebStorage.getInstance().deleteAllData()

                        // Clear all the cookies
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()

                        this.clearCache(true)
                        this.clearFormData()
                        this.clearHistory()
                        this.clearSslPreferences()
                    }
                }.also { webView = it }
            },
            modifier = Modifier
                .alpha(getWebViewAlpha())
        ) { webView ->
            if (lastLoadedUrl == url) return@AndroidView
            lastLoadedUrl = url
            wpComAuthenticator?.authenticateAndLoadUrl(webView, url) ?: webView.loadUrl(url)
            canGoBack = webView.canGoBack()
        }

        if (progressIndicator is Linear) {
            LinearProgressIndicator(
                progress = (progress / 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(getProgressAlpha())
            )

            if (progressIndicator.message != null) {
                Text(
                    text = progressIndicator.message,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(getProgressAlpha())
                )
            }
        } else if (progressIndicator is Circular) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(getProgressAlpha())
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(dimensionResource(id = dimen.major_100)),
                )
                if (progressIndicator.message != null) {
                    Text(text = progressIndicator.message)
                }
            }
        }
    }
}

sealed class WebViewProgressIndicator {
    object None : WebViewProgressIndicator()
    data class Linear(val message: String? = null) : WebViewProgressIndicator()
    data class Circular(val message: String? = null) : WebViewProgressIndicator()
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
