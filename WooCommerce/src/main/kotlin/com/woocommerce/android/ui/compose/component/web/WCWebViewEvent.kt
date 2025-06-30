package com.woocommerce.android.ui.compose.component.web

sealed interface WCWebViewEvent {
    val url: String

    data class UrlLoaded(override val url: String) : WCWebViewEvent
    data class PageFinished(override val url: String) : WCWebViewEvent
    data class UrlFailed(override val url: String, val errorCode: Int?) : WCWebViewEvent
}
