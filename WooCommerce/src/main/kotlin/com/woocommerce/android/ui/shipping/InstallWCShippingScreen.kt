package com.woocommerce.android.ui.shipping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState

@Composable
fun InstallWCShippingScreen(viewModel: InstallWCShippingViewModel) {
    val installWcShippingFlowState by viewModel.viewState.observeAsState()
    installWcShippingFlowState?.let {
        InstallWCShippingScreen(it)
    }
}

@Composable
fun InstallWCShippingScreen(viewState: ViewState) {
    when (viewState) {
        is ViewState.Onboarding -> InstallWcShippingOnboarding(viewState = viewState)
    }
}
