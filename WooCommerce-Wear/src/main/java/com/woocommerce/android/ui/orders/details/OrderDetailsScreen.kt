package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
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
        TimeText()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp)
                .padding(horizontal = 20.dp)
        ) {
            when {
                isLoading -> LoadingScreen()
                order == null -> OrderLoadingFailed()
                else -> OrderDetailsContent(order, modifier)
            }
        }
    }
}

@Composable
fun OrderDetailsContent(
    order: OrderItem,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = order.date,
                color = Color.White
            )
            Text(
                text = order.number,
                color = Color.White
            )
        }
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = order.customerName ?: stringResource(id = R.string.orders_list_guest_customer),
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Text( /* Needs proper handling */
                text = "3 Products",
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Text(
                text = order.total,
                color = Color.White
            )
            Text(
                text = order.status,
                color = Color.White
            )
        }
    }
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
