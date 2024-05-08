package com.woocommerce.android.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.ui.orders.OrdersListViewModel.OrderItem

@Composable
fun OrdersListScreen(viewModel: OrdersListViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    OrdersListScreen(
        orders = viewState?.orders.orEmpty()
    )
}

@Composable
fun OrdersListScreen(
    orders: List<OrderItem>,
    modifier: Modifier = Modifier
) {
    WooTheme {
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            state = listState
        ) {
            items(orders) {
                OrderListItem(
                    modifier = modifier,
                    order = it
                )
            }
        }
    }
}

@Composable
fun OrderListItem(
    modifier: Modifier,
    order: OrderItem
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = order.date,
                )
                Text(
                    text = order.number
                )
            }
            Text(
                text = order.customerName,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = order.total,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = order.status,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun Preview() {
    OrdersListScreen(
        orders = listOf(
            OrderItem(
                date = "25 Feb",
                number = "#125",
                customerName = "John Doe",
                total = "$100.00",
                status = "Processing"
            ),
            OrderItem(
                date = "31 Dec",
                number = "#124",
                customerName = "Jane Doe",
                total = "$200.00",
                status = "Completed"
            ),
            OrderItem(
                date = "4 Oct",
                number = "#123",
                customerName = "John Smith",
                total = "$300.00",
                status = "Pending"
            )
        )
    )
}
