package com.woocommerce.android.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedWebViewFlow @Inject constructor() {
    private val _webViewEventFlow = MutableSharedFlow<WebViewEvent>()
    val webViewEventFlow = _webViewEventFlow.asSharedFlow()

    suspend fun emitEvent(event: WebViewEvent) {
        _webViewEventFlow.emit(event)
    }
}

sealed class WebViewEvent {
    object onWebViewClosed : WebViewEvent()
    data class onPageFinished(val url: String) : WebViewEvent()
    data class onUrlFailed(val url: String, val errorCode: Int?) : WebViewEvent()
    data class onTriggerUrlLoaded(val url: String) : WebViewEvent()
}
