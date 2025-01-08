package com.woocommerce.android.ui.prefs.domain

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.prefs.domain.DomainPurchaseViewModel.ViewState.CheckoutState
import com.woocommerce.android.ui.prefs.domain.DomainPurchaseViewModel.ViewState.LoadingState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun DomainRegistrationCheckoutScreen(
    viewModel: DomainPurchaseViewModel,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent
) {
    viewModel.viewState.observeAsState(LoadingState).value.let { state ->
        Crossfade(targetState = state) { viewState ->
            when (viewState) {
                LoadingState -> ProgressIndicator()
                is CheckoutState -> WebViewPayment(
                    viewState,
                    authenticator,
                    userAgent,
                    viewModel::onPurchaseSuccess,
                    viewModel::onExitTriggered
                )
            }
        }
    }
}

@Composable
private fun WebViewPayment(
    viewState: CheckoutState,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onDomainPurchased: () -> Unit,
    onExitTriggered: () -> Unit
) {
    var domainPurchaseTriggered by remember { mutableStateOf(false) }

    WCWebView(
        url = viewState.startUrl,
        wpComAuthenticator = authenticator,
        userAgent = userAgent,
        onUrlLoaded = { url: String ->
            WooLog.d(T.ONBOARDING, "Webview: $url")
            if (url.contains(viewState.successTriggerKeyword, ignoreCase = true) && !domainPurchaseTriggered) {
                domainPurchaseTriggered = true
                onDomainPurchased()
            } else if (url == viewState.exitTriggerKeyword) {
                onExitTriggered()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
