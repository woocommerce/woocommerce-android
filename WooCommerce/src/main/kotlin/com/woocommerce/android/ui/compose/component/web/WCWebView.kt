package com.woocommerce.android.ui.compose.component.web

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
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
    authenticator: WebViewAuthenticator? = null,
    webViewNavigator: WebViewNavigator = rememberWebViewNavigator(),
    webViewClient: WCWebViewClient = remember { WCWebViewClient() },
    webChromeClient: WCWebChromeClient = remember { WCWebChromeClient() },
    settings: WCWebViewSettings = WCWebViewSettings(),
    progressIndicator: WebViewProgressIndicator = Linear()
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(captureBackPresses && canGoBack) {
        webView?.goBack()
    }

    LaunchedEffect(webView, webViewNavigator) {
        with(webViewNavigator) {
            webView?.handleNavigationEvents()
        }
    }

    LaunchedEffect(webViewClient) {
        webViewClient.eventsObservable
            .collect {
                when (it) {
                    is WCWebViewEvent.UrlLoaded -> onUrlLoaded(it.url)
                    is WCWebViewEvent.PageFinished -> {
                        onPageFinished(it.url)
                        canGoBack = webView?.canGoBack() == true
                    }
                    is WCWebViewEvent.UrlFailed -> onUrlFailed(it.url, it.errorCode)
                }
            }
    }

    webView?.let { webView ->
        LaunchedEffect(url) {
            if (authenticator != null) {
                authenticator.authenticateAndLoadUrl(
                    webView = webView,
                    url = url,
                    webViewEvents = webViewClient.eventsObservable
                )
            } else {
                webView.loadUrl(url)
            }
            canGoBack = webView.canGoBack()
        }

        LaunchedEffect(settings) {
            if (settings.isReadOnly) {
                webView.setOnTouchListener { _, _ -> true }
            }
            webView.setInitialScale(settings.initialScale)

            webView.settings.useWideViewPort = settings.useWideViewPort
            webView.settings.loadWithOverviewMode = settings.loadWithOverviewMode
            webView.settings.javaScriptEnabled = settings.isJavaScriptEnabled
            webView.settings.domStorageEnabled = settings.isDomStorageEnabled
        }
    }

    Box(modifier = modifier) {
        val webViewAlpha by remember {
            derivedStateOf {
                if (progressIndicator is Circular ||
                    progressIndicator is Linear && progressIndicator.message != null
                ) {
                    if (progress == 100) 1f else 0f
                } else {
                    1f
                }
            }
        }
        val progressAlpha by remember {
            derivedStateOf {
                if (progress == 100) 0f else 1f
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    this.webViewClient = webViewClient
                    this.webChromeClient = webChromeClient.apply {
                        onProgressChanged = { newProgress -> progress = newProgress }
                    }

                    this.settings.userAgentString = userAgent.userAgent
                }.also { webView = it }
            },
            modifier = Modifier
                .alpha(webViewAlpha)
        )

        if (progressIndicator is Linear) {
            LinearProgressIndicator(
                progress = (progress / 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(progressAlpha)
            )

            if (progressIndicator.message != null) {
                Text(
                    text = progressIndicator.message,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(progressAlpha)
                )
            }
        } else if (progressIndicator is Circular) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(progressAlpha)
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
