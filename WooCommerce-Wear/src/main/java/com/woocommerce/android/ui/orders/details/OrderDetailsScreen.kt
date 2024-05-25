package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
import com.woocommerce.android.compose.component.ErrorScreen
import com.woocommerce.android.compose.component.LoadingScreen
import com.woocommerce.android.compose.theme.WooColors
import com.woocommerce.android.compose.theme.WooTheme
import com.woocommerce.android.compose.theme.WooTypography
import com.woocommerce.android.ui.orders.FormatOrderData.OrderItem
import com.woocommerce.android.ui.orders.FormatOrderData.ProductItem

@Composable
fun OrderDetailsScreen(viewModel: OrderDetailsViewModel) {
    LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)
    val viewState = viewModel.viewState.observeAsState()
    OrderDetailsScreen(
        order = viewState.value?.orderItem,
        isLoading = viewState.value?.isLoading ?: false,
        onRetryClicked = viewModel::reloadData
    )
}

@Composable
fun OrderDetailsScreen(
    order: OrderItem?,
    isLoading: Boolean,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            when {
                isLoading -> LoadingScreen()
                order == null -> ErrorScreen(
                    errorText = stringResource(id = R.string.order_details_failed_to_load),
                    onRetryClicked = onRetryClicked
                )

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 40.dp)
    ) {
        OrderHeader(modifier, order)
        Spacer(modifier = modifier.padding(10.dp))
        OrderProductsList(order.products, modifier)
        Divider()
    }
}

@Composable
private fun OrderHeader(
    modifier: Modifier,
    order: OrderItem
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = order.number,
            fontWeight = FontWeight.Bold,
            color = WooColors.woo_purple_20,
            modifier = modifier.padding(bottom = 10.dp)
        )
        Text(
            text = order.date,
            style = WooTypography.body2,
            fontWeight = FontWeight.Bold,
            color = WooColors.woo_gray_alpha,
        )
    }
    Spacer(modifier = modifier.padding(5.dp))
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = order.customerName,
            style = WooTypography.body1,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = order.total,
            style = WooTypography.title2,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = order.status,
            style = WooTypography.body2,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OrderProductsList(
    products: List<ProductItem>?,
    modifier: Modifier
) {
    products
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            Text(
                text = pluralizedProductsText(products = it),
                textAlign = TextAlign.Center,
                style = WooTypography.body1,
                fontWeight = FontWeight.Bold,
                color = WooColors.woo_gray_alpha,
                modifier = modifier.fillMaxWidth()
            )
        }
    when {
        products == null -> Text(
            text = stringResource(id = R.string.order_details_products_failed),
            style = WooTypography.caption1,
            color = WooColors.woo_gray_alpha,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        products.isEmpty() -> Text(
            text = stringResource(id = R.string.order_details_no_products_found),
            style = WooTypography.caption1,
            color = WooColors.woo_gray_alpha,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        else -> products.forEach { product ->
            Box(
                modifier = modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Row(modifier = modifier.fillMaxWidth()) {
                    Column {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = modifier
                                .padding(2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(WooColors.woo_purple_alpha)
                        ) {
                            Text(
                                text = product.amount.toString(),
                                textAlign = TextAlign.Center,
                                color = WooColors.woo_purple_20
                            )
                        }
                    }
                    Spacer(modifier = modifier.padding(2.dp))
                    Column {
                        Text(
                            text = product.name,
                            maxLines = 2,
                            color = Color.White,
                            style = WooTypography.body1,
                            modifier = modifier.fillMaxWidth()
                        )
                        Text(
                            text = product.total,
                            style = WooTypography.body2,
                            color = WooColors.woo_gray_alpha,
                        )
                    }
                }
            }
            Spacer(modifier = modifier.padding(4.dp))
        }
    }
}

@Composable
@ReadOnlyComposable
private fun pluralizedProductsText(products: List<ProductItem>): String {
    val amount = products.size
    return if (amount == 1) {
        stringResource(id = R.string.order_details_single_product_amount, amount)
    } else {
        stringResource(id = R.string.order_details_multiple_products_amount, amount)
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun Preview() {
    OrderDetailsScreen(
        isLoading = false,
        onRetryClicked = {},
        order = OrderItem(
            id = 0L,
            date = "25 Feb",
            number = "#125",
            customerName = "John Doe",
            total = "$100.00",
            status = "Processing",
            products = listOf(
                ProductItem(
                    amount = 3,
                    total = "$100.00",
                    name = "Product very very very very very very long name"
                ),
                ProductItem(
                    amount = 2,
                    total = "$200.00",
                    name = "Product 2"
                )
            )
        )
    )
}
