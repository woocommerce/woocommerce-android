package com.woocommerce.android.ui.onboarding.aboutyourstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.onboarding.aboutyourstore.AboutYourStoreViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.onboarding.aboutyourstore.AboutYourStoreViewModel.ViewState.WebViewState
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun AboutYourStoreScreen(
    viewModel: AboutYourStoreViewModel,
    userAgent: UserAgent,
    authenticator: WebViewAuthenticator
) {
    viewModel.viewState.observeAsState(LoadingState).value.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = R.string.store_onboarding_task_about_your_store_toolbar_title),
                onNavigationButtonClick = viewModel::onBackPressed
            )
        }) { padding ->
            when (state) {
                is LoadingState -> {
                    ProgressIndicator(
                        modifier = Modifier
                            .padding(padding)
                    )
                }
                is WebViewState -> {
                    WCWebView(
                        url = state.url,
                        userAgent = userAgent,
                        authenticator = authenticator,
                        modifier = Modifier
                            .background(MaterialTheme.colors.surface)
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator(modifier: Modifier) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = modifier
                .align(Alignment.Center)
        )
    }
}
