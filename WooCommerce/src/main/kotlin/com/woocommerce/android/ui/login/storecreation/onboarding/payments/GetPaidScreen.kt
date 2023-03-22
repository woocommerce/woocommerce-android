package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.WebViewProgressIndicator.Circular
import com.woocommerce.android.ui.compose.component.WebViewProgressIndicator.Linear
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun GetPaidScreen(
    viewModel: GetPaidViewModel,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState(ViewState("", false)).value.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = string.store_onboarding_task_payments_setup_title),
                onNavigationButtonClick = viewModel::onBackPressed
            )
        }) { padding ->
            WpAdmin(
                state,
                userAgent,
                authenticator,
                viewModel::onUrlLoaded,
                modifier = Modifier.background(MaterialTheme.colors.surface)
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun WpAdmin(
    viewState: ViewState,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator,
    onUrlLoaded: (String) -> Unit,
    modifier: Modifier
) {
    WooLog.d(WooLog.T.UTILS, "GetPaidScreen: url = $viewState.url")
    WCWebView(
        url = viewState.url,
        userAgent = userAgent,
        wpComAuthenticator = authenticator,
        onUrlLoaded = onUrlLoaded,
        progressIndicator = if (viewState.showSpinner) Circular() else Linear(),
        modifier = modifier
    )
}
