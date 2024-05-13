package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooTheme
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
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    order: OrderItem?
) {
    WooTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize()
        ) {
            when {
                isLoading -> LoadingScreen()
                order == null -> OrderLoadingFailed()
                else -> OrderDetailsContent(order)
            }
        }
    }
}

@Composable
fun OrderDetailsContent(order: OrderItem) {
    Text(
        text = order.number,
        color = Color.White
    )
}

@Composable
fun OrderLoadingFailed() {
    Text(
        text = "Failed to load Order data",
        color = Color.White
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun Preview() {
    OrderDetailsScreen(
        isLoading = false,
        order = OrderItem(
            id = 0L,
            date = "25 Feb",
            number = "#125",
            customerName = "John Doe",
            total = "$100.00",
            status = "Processing"
        )
    )
}
