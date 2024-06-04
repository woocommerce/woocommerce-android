package com.woocommerce.android.ui.woopos.home

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel

internal const val HOME_ROUTE = "home"

internal fun NavGraphBuilder.homeScreen(onCheckoutClick: () -> Unit) {
    composable(HOME_ROUTE) {
        val cartViewModel: WooPosCartViewModel = hiltViewModel()
        val productsViewModel: ProductSelectorViewModel = hiltViewModel()

        WooPosHomeScreen(
            cartViewModel = cartViewModel,
            productsViewModel = productsViewModel,
            onCheckoutClick = onCheckoutClick
        )
    }
}
