package com.woocommerce.android.ui.bookings.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListSelectionScreen
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListSelectionViewModel
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerSelected
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun BookingCustomerFilterPage(
    onCustomerSelected: (Order.Customer) -> Unit,
) {
    val viewModel = hiltViewModel<CustomerListSelectionViewModel>()

    CustomerListSelectionScreen(
        viewModel = viewModel,
        handleInsets = false,
        showToolbar = false
    )

    HandleEvents(viewModel.event, onCustomerSelected)
}

@Composable
private fun HandleEvents(
    event: LiveData<Event>,
    onCustomerSelected: (Order.Customer) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is CustomerSelected -> onCustomerSelected(event.customer)
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}
