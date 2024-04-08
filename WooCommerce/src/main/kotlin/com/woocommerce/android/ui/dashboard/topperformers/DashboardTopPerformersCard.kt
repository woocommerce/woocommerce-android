package com.woocommerce.android.ui.dashboard.topperformers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils

@Composable
fun DashboardTopPerformersCard(
    selectedSite: SelectedSite,
    dateUtils: DateUtils,
    viewModel: DashBoardTopPerformersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {

    val dateRange by viewModel.selectedDateRange.observeAsState()
    val topPerformersState by viewModel.topPerformersState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateTopPerformers.observeAsState()
    val context = LocalContext.current


}
