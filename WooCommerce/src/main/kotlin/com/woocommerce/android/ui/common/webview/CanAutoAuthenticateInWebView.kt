package com.woocommerce.android.ui.common.webview

import javax.inject.Inject

/**
 * A utility use-case to allow consumers to know beforehand if a URL can be auto-authenticated in a WebView.
 */
class CanAutoAuthenticateInWebView @Inject constructor(
    private val authenticationFlowResolver: WebViewAuthenticationFlowResolver
) {
    operator fun invoke(url: String): Boolean {
        val authenticationFlow = authenticationFlowResolver.resolve(url)
        return authenticationFlow != WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.None
    }
}
