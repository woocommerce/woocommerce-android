package com.woocommerce.android.ui.payments.hub.depositsummary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val ANIM_DURATION_MILLIS = 128

@Composable
fun PaymentsHubDepositSummaryView(
    viewModel: PaymentsHumDepositSummaryViewModel = viewModel()
) {
    viewModel.viewState.observeAsState().let {
        val value = it.value
        when (value) {
            is PaymentsHubDepositSummaryState.Success -> PaymentsHubDepositSummaryView(value.overview)
            null,
            PaymentsHubDepositSummaryState.Loading,
            is PaymentsHubDepositSummaryState.Error -> {
                // show nothing
            }
        }
    }
}

@Composable
fun PaymentsHubDepositSummaryView(
    overview: PaymentsHubDepositSummaryState.Overview
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Card(
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                isExpanded = !isExpanded
            }
    ) {

    }
}
