package com.woocommerce.android.ui.woopos.common.composeui.component.authenticatedwebview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.compose.component.web.WebViewProgressIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun WooPosAuthenticatedWebView(
    url: String,
    title: String,
    authenticator: WebViewAuthenticator,
    userAgent: UserAgent,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = title,
            onBackClicked = onBackClick,
        )
        WCWebView(
            url = url,
            userAgent = userAgent,
            authenticator = authenticator,
            modifier = Modifier.fillMaxSize(),
            progressIndicator = WebViewProgressIndicator.Linear(),
        )
    }
}
