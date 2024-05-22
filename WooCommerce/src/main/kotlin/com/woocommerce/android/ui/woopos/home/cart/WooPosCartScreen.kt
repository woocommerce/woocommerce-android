package com.woocommerce.android.ui.woopos.home.cart

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.home.products.ProductSelector
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel
import com.woocommerce.android.ui.woopos.util.WooPosPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosCartScreen(
    cartViewModel: WooPosCartViewModel,
    productsViewModel: ProductSelectorViewModel,
    onCheckoutClick: () -> Unit,
) {
    WooPosCartScreen(
        onCheckoutClick = onCheckoutClick,
        productsState = productsViewModel.viewState,
        onEndOfProductsGridReached = productsViewModel::onEndOfProductsGridReached,
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun WooPosCartScreen(
    onCheckoutClick: () -> Unit,
    productsState: StateFlow<ProductSelectorViewModel.ViewState>,
    onEndOfProductsGridReached: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close POS"
                        )
                    },
                    title = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "⚠️ Reader not connected",
                            style = MaterialTheme.typography.h5,
                            color = MaterialTheme.colors.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    },
                    actions = {
                        Text(text = "History")
                    }
                )
            },
        ) {
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                ProductSelector(productsState, onEndOfProductsGridReached)
                Cart(onCheckoutClick)
            }
        }
    }
}

@Composable
private fun Cart(onButtonClicked: () -> Unit) {
    Column {
        Text(
            text = "Cart",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colors.primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onButtonClicked,
        ) {
            Text("Checkout")
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    val productState = MutableStateFlow(
        ProductSelectorViewModel.ViewState(
            products = listOf(
                ProductSelectorViewModel.ListItem(1, "Product 1"),
                ProductSelectorViewModel.ListItem(2, "Product 2"),
                ProductSelectorViewModel.ListItem(3, "Product 3"),
            )
        )
    )
    WooPosCartScreen(onCheckoutClick = {}, productsState = productState, onEndOfProductsGridReached = {})
}
