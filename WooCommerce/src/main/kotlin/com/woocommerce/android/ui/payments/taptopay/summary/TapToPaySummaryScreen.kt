package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.compose.runtime.Composable

@Composable
fun TapToPaySummaryScreen(viewModel: TapToPaySummaryViewModel) {
    val state by viewModel.uiState.collectAsState()
}
