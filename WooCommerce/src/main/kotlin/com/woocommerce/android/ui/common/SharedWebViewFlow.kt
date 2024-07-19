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
    data class onPageFinished(val url: String) : WebViewEvent()
}
