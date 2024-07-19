package com.woocommerce.android.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton class responsible for managing web view events within the application's multiple available WebViews
 * (e.g: `WPComWebViewScreen`, `ExitAwareWebViewScreen`).
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

/**
 * Sealed class representing different types of web view events that can occur within the application.
 */
sealed class WebViewEvent {
    // Represents the event of a web view being closed.
    object OnWebViewClosed : WebViewEvent()

    // Represents the event of a page finishing loading within a web view.
    data class OnPageFinished(val url: String) : WebViewEvent()

    // Represents the event of a URL failing to load within a web view, optionally including an error code.
    data class OnUrlFailed(val url: String, val errorCode: Int?) : WebViewEvent()

    // Represents the event of a specific URL being successfully loaded within a web view.
    data class OnTriggerUrlLoaded(val url: String) : WebViewEvent()
}
