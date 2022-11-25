package com.woocommerce.android.ui.common.wpcomwebview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel.DisplayMode.MODAL
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel.DisplayMode.REGULAR
import com.woocommerce.android.ui.compose.component.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun WPComWebViewScreen(viewViewModel: WPComWebViewViewModel) {
    WPComWebViewScreen(
        viewState = viewViewModel.viewState,
        wpcomWebViewAuthenticator = viewViewModel.wpComWebViewAuthenticator,
        userAgent = viewViewModel.userAgent,
        onUrlLoaded = viewViewModel::onUrlLoaded,
        onClose = viewViewModel::onClose
    )
}

@Composable
fun WPComWebViewScreen(
    viewState: WPComWebViewViewModel.ViewState,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.title ?: stringResource(id = R.string.app_name),
                displayMode = viewState.displayMode,
                onCloseClick = onClose
            )
        }
    ) { paddingValues ->
        WCWebView(
            url = viewState.urlToLoad,
            userAgent = userAgent,
            wpComAuthenticator = wpcomWebViewAuthenticator,
            onUrlLoaded = onUrlLoaded,
            captureBackPresses = viewState.captureBackButton,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

@Composable
private fun Toolbar(
    title: String,
    displayMode: WPComWebViewViewModel.DisplayMode,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = when (displayMode) {
                        REGULAR -> Icons.Filled.ArrowBack
                        MODAL -> Icons.Filled.Clear
                    },
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}
