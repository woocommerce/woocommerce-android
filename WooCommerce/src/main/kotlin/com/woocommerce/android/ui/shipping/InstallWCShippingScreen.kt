package com.woocommerce.android.ui.shipping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState

@Composable
fun InstallWCShippingScreen(viewModel: InstallWCShippingViewModel) {
    val installWcShippingFlowState by viewModel.viewState.observeAsState()
    installWcShippingFlowState?.let {
        InstallWCShippingScreen(it)
    }
}

@Composable
fun InstallWCShippingScreen(viewState: ViewState) {
    Box(modifier = Modifier.background(color = MaterialTheme.colors.surface)) {
        when (viewState) {
            is ViewState.Onboarding -> InstallWcShippingOnboarding(viewState = viewState)
            is InstallationState -> InstallWCShippingFlow(viewState = viewState)
        }
    }
}
