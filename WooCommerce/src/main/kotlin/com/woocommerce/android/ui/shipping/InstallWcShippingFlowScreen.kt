package com.woocommerce.android.ui.shipping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.ViewState

@Composable
fun InstallWcShippingFlowScreen(viewModel: InstallWcShippingFlowViewModel) {
    val installWcShippingFlowState by viewModel.viewState.observeAsState()
    installWcShippingFlowState?.let {
        InstallWcShippingFlowScreen(it)
    }
}

@Composable
fun InstallWcShippingFlowScreen(viewState: ViewState) {
    when (viewState) {
        is ViewState.Onboarding -> InstallWcShippingOnboarding(viewState = viewState)
    }
}
