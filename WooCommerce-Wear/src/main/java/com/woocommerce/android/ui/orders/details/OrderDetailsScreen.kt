package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.ui.orders.ParseOrderData
import com.woocommerce.android.ui.orders.ParseOrderData.OrderItem

@Composable
fun OrderDetailsScreen(viewModel: OrderDetailsViewModel) {
    val viewState = viewModel.viewState.observeAsState()
    OrderDetailsScreen(
        isLoading = viewState.value?.isLoading ?: false,
        order = viewState.value?.orderItem
    )
}

@Composable
fun OrderDetailsScreen(
    isLoading: Boolean,
    order: OrderItem?
) {
    Column {
        when {
            isLoading -> LoadingScreen()
            order == null -> OrderLoadingFailed()
            else -> OrderDetailsContent(order)
        }
    }
}

@Composable
fun OrderDetailsContent(order: OrderItem) {
    Text(order.number)
}

@Composable
fun OrderLoadingFailed() {
    Text("Failed to load Order data")
}
