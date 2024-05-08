package com.woocommerce.android.ui.orders

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.theme.WooTheme

@Composable
fun OrdersListScreen(viewModel: OrdersListViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    OrdersListScreen(
        orders = viewState?.orders.orEmpty()
    )
}

@Composable
fun OrdersListScreen(
    orders: List<String>
) {
    WooTheme {
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            state = listState
        ) {
            items(orders) { order ->
                Text(
                    text = order,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun Preview() {
    OrdersListScreen(orders = listOf("Order 1", "Order 2", "Order 3"))
}
