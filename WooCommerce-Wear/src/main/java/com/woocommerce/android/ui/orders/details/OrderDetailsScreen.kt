package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooColors
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography
import com.woocommerce.android.ui.orders.FormatOrderData.OrderItem

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
        Box(
            contentAlignment = Alignment.TopCenter,
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
                text = order.customerName,
                style = WooTypography.title3,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text( // Needs proper handling
                text = "3 Products",
                textAlign = TextAlign.Center,
                color = Color.White
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
