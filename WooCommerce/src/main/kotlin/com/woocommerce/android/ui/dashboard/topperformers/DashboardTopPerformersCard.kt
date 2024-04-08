package com.woocommerce.android.ui.dashboard.topperformers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardTopPerformersCard(
    viewModel: DashBoardTopPerformersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {

    val dateRange by viewModel.selectedDateRange.observeAsState()
    val topPerformersState by viewModel.topPerformersState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateTopPerformers.observeAsState()
    val context = LocalContext.current

}
