package com.woocommerce.android.ui.products.details.webview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ProductDetailWebViewScreen(
    viewModel: ProductDetailWebViewViewModel,
    showNavigationIcon: Boolean
) {
    viewModel.viewState.observeAsState().value?.let {
        ProductDetailWebViewScreen(
            viewState = it,
            showNavigationIcon = showNavigationIcon,
            webViewAuthenticator = viewModel.webViewAuthenticator,
            userAgent = viewModel.userAgent,
        )
    }
}

@Composable
private fun ProductDetailWebViewScreen(
    viewState: ProductDetailWebViewViewModel.ViewState,
    showNavigationIcon: Boolean,
    webViewAuthenticator: WebViewAuthenticator,
    userAgent: UserAgent
) {
    BackHandler(onBack = viewState.onBackClick)

    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.title,
                navigationIcon = if (showNavigationIcon) Icons.AutoMirrored.Default.ArrowBack else null,
                onNavigationButtonClick = viewState.onBackClick
            )
        }
    ) {
        WCWebView(
            url = viewState.urlToLoad,
            authenticator = webViewAuthenticator,
            userAgent = userAgent,
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        )
    }
}
