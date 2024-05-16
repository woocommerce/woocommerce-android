package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooColors
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography
import com.woocommerce.android.ui.orders.FormatOrderData.OrderItem

@Composable
fun OrdersListScreen(viewModel: OrdersListViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    OrdersListScreen(
        isLoading = viewState?.isLoading ?: false,
        orders = viewState?.orders.orEmpty(),
        onOrderClicked = viewModel::onOrderItemClick
    )
}

@Composable
fun OrdersListScreen(
    isLoading: Boolean,
    orders: List<OrderItem>,
    onOrderClicked: (orderId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    WooTheme {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(id = R.string.orders_list_screen_title),
                    style = WooTypography.body1,
                    color = WooColors.woo_gray_alpha,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                if (isLoading) {
                    LoadingScreen()
                } else {
                    OrdersLazyColumn(orders, onOrderClicked, modifier)
                }
            }
        }
    }
}

@Composable
private fun OrdersLazyColumn(
    orders: List<OrderItem>,
    onOrderClicked: (orderId: Long) -> Unit,
    modifier: Modifier
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0),
        state = rememberScalingLazyListState(
            initialCenterItemIndex = 0
        )
    ) {
        items(orders) {
            OrderListItem(
                order = it,
                onOrderClicked = onOrderClicked,
                modifier = modifier
            )
        }
    }
}

@Composable
fun OrderListItem(
    order: OrderItem,
    onOrderClicked: (orderId: Long) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.DarkGray)
            .padding(10.dp)
            .fillMaxWidth()
            .clickable { onOrderClicked(order.id) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = order.date,
                    color = WooColors.woo_purple_20
                )
                Text(
                    text = order.number,
                    color = WooColors.woo_gray_alpha
                )
            }
            Text(
                text = order.customerName,
                style = WooTypography.body1,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = order.total,
                style = WooTypography.body1,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = order.status,
                style = WooTypography.caption1,
                color = WooColors.woo_gray_alpha,
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
        isLoading = false,
        onOrderClicked = {},
        orders = listOf(
            OrderItem(
                id = 0L,
                date = "25 Feb",
                number = "#125",
                customerName = "John Doe",
                total = "$100.00",
                status = "Processing"
            ),
            OrderItem(
                id = 1L,
                date = "31 Dec",
                number = "#124",
                customerName = "Jane Doe",
                total = "$200.00",
                status = "Completed"
            ),
            OrderItem(
                id = 2L,
                date = "4 Oct",
                number = "#123",
                customerName = "John Smith",
                total = "$300.00",
                status = "Pending"
            )
        )
    )
}