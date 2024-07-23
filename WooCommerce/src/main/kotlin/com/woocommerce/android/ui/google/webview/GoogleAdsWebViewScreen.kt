package com.woocommerce.android.ui.google.webview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.DisplayMode.MODAL
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.DisplayMode.REGULAR
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun GoogleAdsWebViewScreen(viewViewModel: GoogleAdsWebViewViewModel) {
    GoogleAdsWebViewScreen(
        viewState = viewViewModel.viewState,
        userAgent = viewViewModel.userAgent,
        onUrlLoaded = viewViewModel::onUrlLoaded,
        onClose = viewViewModel::onClose
    )
}

@Composable
fun GoogleAdsWebViewScreen(
    viewState: GoogleAdsWebViewViewModel.ViewState,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onClose: () -> Unit,
    clearCache: Boolean = false
) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.title ?: stringResource(id = R.string.app_name),
                onNavigationButtonClick = onClose,
                navigationIcon = when (viewState.displayMode) {
                    REGULAR -> Icons.AutoMirrored.Filled.ArrowBack
                    MODAL -> Icons.Filled.Clear
                }
            )
        }
    ) { paddingValues ->
        WCWebView(
            url = viewState.urlToLoad,
            userAgent = userAgent,
            onUrlLoaded = onUrlLoaded,
            captureBackPresses = viewState.captureBackButton,
            clearCache = clearCache,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}
