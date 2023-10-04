package com.woocommerce.android.ui.payments.taptopay.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun TapToPayAboutScreen(viewModel: TapToPayAboutViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TapToPayAboutScreen(
            onBackClick = viewModel::onBackClicked,
            state
        )
    }
}

@Composable
fun TapToPayAboutScreen(
    onBackClick: () -> Unit,
    state: TapToPayAboutViewModel.UiState,
) {

}
