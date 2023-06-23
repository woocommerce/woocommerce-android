package com.woocommerce.android.ui.blaze

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeWebViewViewModel.BlazeWebViewState
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun BlazeWebViewScreen(
    viewState: BlazeWebViewState,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    onPageFinished: (String) -> Unit,
    onClose: () -> Unit,
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
        WCWebView(
            url = viewState.urlToLoad,
            userAgent = userAgent,
            wpComAuthenticator = wpcomWebViewAuthenticator,
            onPageFinished = onPageFinished,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}
