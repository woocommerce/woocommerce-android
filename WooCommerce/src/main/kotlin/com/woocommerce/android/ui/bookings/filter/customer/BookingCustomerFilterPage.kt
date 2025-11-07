package com.woocommerce.android.ui.bookings.filter.customer

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListSelectionScreen

@Composable
fun BookingCustomerFilterPage(
    onCustomerSelected: (Order.Customer) -> Unit,
) {
    val viewModel = hiltViewModel { factory: BookingCustomerFilterViewModel.Factory ->
        factory.create(onCustomerSelected)
    }

    CustomerListSelectionScreen(
        viewModel = viewModel,
        handleInsets = false,
        showToolbar = false
    )
}
