package com.woocommerce.android.ui.woopos.home.cart

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel

internal const val CART_ROUTE = "cart"

internal fun NavGraphBuilder.cartScreen(
    onCheckoutClick: () -> Unit
) {
    composable(CART_ROUTE) {
        val cartViewModel: WooPosCartViewModel = hiltViewModel()
        val productsViewModel: ProductSelectorViewModel = hiltViewModel()

        WooPosCartScreen(
            cartViewModel = cartViewModel,
            productsViewModel = productsViewModel,
            onCheckoutClick = onCheckoutClick
        )
    }
}
