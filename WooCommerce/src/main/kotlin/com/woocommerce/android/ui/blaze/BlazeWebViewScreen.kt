package com.woocommerce.android.ui.blaze

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeWebViewViewModel.BlazeCreationViewState
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun BlazeWebViewScreen(
    viewModel: BlazeWebViewViewModel,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
    onClose: () -> Unit
) {
    viewModel.viewState.observeAsState().value?.let {
        BlazeWebViewScreen(
            viewState = it,
            userAgent = userAgent,
            wpcomWebViewAuthenticator = wpcomWebViewAuthenticator,
            activityRegistry = activityRegistry,
            onClose = onClose
        )
    }
}

@Composable
fun BlazeWebViewScreen(
    viewState: BlazeCreationViewState,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.more_menu_button_blaze),
                onNavigationButtonClick = onClose,
                navigationIcon = Filled.ArrowBack
            )
        }
    ) { paddingValues ->
        when (viewState) {
            BlazeCreationViewState.Intro -> TODO()
            is BlazeCreationViewState.BlazeWebViewState -> WCWebView(
                url = viewState.urlToLoad,
                userAgent = userAgent,
                wpComAuthenticator = wpcomWebViewAuthenticator,
                onPageFinished = viewState.onPageFinished,
                activityRegistry = activityRegistry,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}
