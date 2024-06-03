package com.woocommerce.android.ui.woopos.home.cart

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.home.products.ListItem
import com.woocommerce.android.ui.woopos.home.products.ProductSelector
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel
import com.woocommerce.android.ui.woopos.home.products.ViewState
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
    productsState: StateFlow<ViewState>,
    onEndOfProductsGridReached: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProductSelector(productsState, onEndOfProductsGridReached)
        Cart(onCheckoutClick)
    }
}

@Composable
private fun Cart(onButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
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
        ViewState(
            products = listOf(
                ListItem(1, "Product 1"),
                ListItem(2, "Product 2"),
                ListItem(3, "Product 3"),
            )
        )
    )
    WooPosCartScreen(
        onCheckoutClick = {},
        productsState = productState,
        onEndOfProductsGridReached = {}
    )
}
