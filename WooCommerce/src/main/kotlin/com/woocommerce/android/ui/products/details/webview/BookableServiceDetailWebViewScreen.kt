package com.woocommerce.android.ui.products.details.webview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun BookableServiceDetailWebViewScreen(
    viewModel: BookableServiceDetailWebViewViewModel,
    showNavigationIcon: Boolean
) {
    viewModel.viewState.observeAsState().value?.let {
        BookableServiceDetailWebViewScreen(
            viewState = it,
            showNavigationIcon = showNavigationIcon,
            webViewAuthenticator = viewModel.webViewAuthenticator,
            userAgent = viewModel.userAgent,
        )
    }
}

@Composable
private fun BookableServiceDetailWebViewScreen(
    viewState: BookableServiceDetailWebViewViewModel.ViewState,
    showNavigationIcon: Boolean,
    webViewAuthenticator: WebViewAuthenticator,
    userAgent: UserAgent
) {
    BackHandler(onBack = viewState.onBackClick)

    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.title,
                navigationIcon = if (showNavigationIcon) ImageVector.vectorResource(R.drawable.ic_back_24dp) else null,
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
