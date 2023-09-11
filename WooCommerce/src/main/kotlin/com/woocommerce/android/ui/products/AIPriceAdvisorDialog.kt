package com.woocommerce.android.ui.products

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun AIPriceAdvisorDialog(viewModel: AIPriceAdvisorViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Text(state.generatedAdvice)
    }
}
