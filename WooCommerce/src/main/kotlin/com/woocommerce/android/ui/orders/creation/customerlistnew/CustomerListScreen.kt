package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel
) {
    val state by viewModel.viewState.observeAsState(CustomerListViewModel.ViewState.Loading)
    print(state)
}
