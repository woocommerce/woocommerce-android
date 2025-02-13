package com.woocommerce.android.ui.compose.component.web

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.CallSuper
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class WCWebViewClient : WebViewClient() {
    private val _eventsObservable = MutableSharedFlow<WCWebViewEvent>(extraBufferCapacity = Int.MAX_VALUE)
    val eventsObservable: Flow<WCWebViewEvent> = _eventsObservable.asSharedFlow()

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        url?.let { _eventsObservable.tryEmit(WCWebViewEvent.UrlLoaded(it)) }
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        url?.let { _eventsObservable.tryEmit(WCWebViewEvent.UrlLoaded(it)) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        url?.let { _eventsObservable.tryEmit(WCWebViewEvent.PageFinished(it)) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        request?.url?.let { url ->
            _eventsObservable.tryEmit(WCWebViewEvent.UrlFailed(url.toString(), error?.errorCode))
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        request?.url?.let { url ->
            _eventsObservable.tryEmit(WCWebViewEvent.UrlFailed(url.toString(), errorResponse?.statusCode))
        }
    }

    @CallSuper
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return if (request?.url?.toString() == WebViewAuthenticator.JETPACK_SSO_TEMP_REDIRECT_URL) {
            // Cancel loading for the temporary redirect URL
            true
        } else {
            false
        }
    }
}
