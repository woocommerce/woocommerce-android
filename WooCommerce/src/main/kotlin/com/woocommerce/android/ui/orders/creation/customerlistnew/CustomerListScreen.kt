package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel
) {
    val state by viewModel.viewState.observeAsState(CustomerListViewModel.ViewState.Loading)
    CustomerListScreen(
        state,
        viewModel::onCustomerClick
    )
}

@Composable
@Suppress("UnusedPrivateMember", "EmptyFunctionBlock")
fun CustomerListScreen(
    state: CustomerListViewModel.ViewState,
    onCustomerClick: ((Long) -> Unit?)? = null
) {
}
