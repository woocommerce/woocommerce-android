package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import android.annotation.SuppressLint
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
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.WebViewState
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun GetPaidScreen(
    viewModel: GetPaidViewModel,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState(LoadingState).value.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = string.store_onboarding_task_payments_setup_title),
                onNavigationButtonClick = viewModel::onBackPressed
            )
        }) { padding ->
            when (state) {
                is LoadingState -> {
                    ProgressIndicator(modifier = Modifier
                        .padding(padding)
                    )
                }
                is WebViewState -> {
                    WpAdmin(
                        state,
                        userAgent,
                        if (state.shouldAuthenticate) authenticator else null,
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
    Box(modifier = Modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxSize()) {
        CircularProgressIndicator(
            modifier = modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun WpAdmin(
    viewState: WebViewState,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator?,
    modifier: Modifier
) {
    WCWebView(
        url = viewState.url,
        userAgent = userAgent,
        wpComAuthenticator = authenticator,
        modifier = modifier
    )
}
