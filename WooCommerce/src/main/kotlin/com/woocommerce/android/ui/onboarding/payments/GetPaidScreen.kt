package com.woocommerce.android.ui.onboarding.payments

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun GetPaidScreen(
    viewModel: GetPaidViewModel,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState(null).value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = string.store_onboarding_task_payments_setup_title),
                onNavigationButtonClick = viewModel::onBackPressed
            )
        }) { padding ->
            WCWebView(
                url = state.url,
                userAgent = userAgent,
                wpComAuthenticator = if (state.shouldAuthenticate) authenticator else null,
                onUrlLoaded = viewModel::onUrlLoaded,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
