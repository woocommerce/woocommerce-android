package com.woocommerce.android.ui.woopos.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.home.cart.Cart
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.products.ListItem
import com.woocommerce.android.ui.woopos.home.products.ProductSelector
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel
import com.woocommerce.android.ui.woopos.home.products.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosHomeScreen(
    cartViewModel: WooPosCartViewModel,
    productsViewModel: ProductSelectorViewModel,
    onCheckoutClick: () -> Unit,
) {
    WooPosHomeScreen(
        onCheckoutClick = onCheckoutClick,
        productsState = productsViewModel.viewState,
        onEndOfProductsGridReached = productsViewModel::onEndOfProductsGridReached,
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun WooPosHomeScreen(
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
@WooPosPreview
fun WooPosHomeScreenPreview() {
    val productState = MutableStateFlow(
        ViewState(
            products = listOf(
                ListItem(1, "Product 1"),
                ListItem(2, "Product 2"),
                ListItem(3, "Product 3"),
            )
        )
    )
    WooPosHomeScreen(
        onCheckoutClick = {},
        productsState = productState,
        onEndOfProductsGridReached = {}
    )
}
