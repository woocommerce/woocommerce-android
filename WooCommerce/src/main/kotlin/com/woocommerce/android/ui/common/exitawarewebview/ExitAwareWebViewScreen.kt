package com.woocommerce.android.ui.common.exitawarewebview

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
import com.woocommerce.android.ui.common.exitawarewebview.ExitAwareWebViewViewModel.DisplayMode.MODAL
import com.woocommerce.android.ui.common.exitawarewebview.ExitAwareWebViewViewModel.DisplayMode.REGULAR
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ExitAwareWebViewScreen(viewViewModel: ExitAwareWebViewViewModel) {
    ExitAwareWebViewScreen(
        viewState = viewViewModel.viewState,
        userAgent = viewViewModel.userAgent,
        onUrlLoaded = viewViewModel::onUrlLoaded,
        onPageFinished = viewViewModel::onPageFinished,
        onClose = viewViewModel::onClose
    )
}

@Composable
fun ExitAwareWebViewScreen(
    viewState: ExitAwareWebViewViewModel.ViewState,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onPageFinished: (String) -> Unit,
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
            onPageFinished = onPageFinished,
            captureBackPresses = viewState.captureBackButton,
            clearCache = clearCache,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}
