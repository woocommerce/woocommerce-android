package com.woocommerce.android.ui.common.wpcomwebview

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton class responsible for managing web view events within the application's WebView
 * (e.g: `WPComWebView`).
 */
@Singleton
class SharedWebViewFlow @Inject constructor() {
    private val _webViewEventFlow = MutableSharedFlow<WebViewEvent>()
    val webViewEventFlow = _webViewEventFlow.asSharedFlow()

    /**
     * Emits a [WebViewEvent] to the shared flow, allowing subscribers to react to web view events.
     *
     * @param event The [WebViewEvent] to be emitted.
     */
    suspend fun emitEvent(event: WebViewEvent) {
        _webViewEventFlow.emit(event)
    }
}

sealed class WebViewEvent {
    object OnWebViewClosed : WebViewEvent()
    data class OnPageFinished(val url: String) : WebViewEvent()
    data class OnUrlFailed(val url: String, val errorCode: Int?) : WebViewEvent()
    data class OnTriggerUrlLoaded(val url: String) : WebViewEvent()
}
