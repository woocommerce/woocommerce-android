package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel

@Composable
fun LaunchStoreScreen(viewModel: LaunchStoreViewModel) {
    viewModel.viewState.observeAsState(InstallationViewModel.ViewState.InitialState).value.let { state ->

    }
}
