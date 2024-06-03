package com.woocommerce.android.ui.woopos.cart

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal const val CART_ROUTE = "cart"

internal fun NavGraphBuilder.cartScreen(onCheckoutClick: () -> Unit) {
    composable(CART_ROUTE) {
        val viewModel: WooPosCartViewModel = hiltViewModel()

        WooPosCartScreen(
            viewModel = viewModel,
            onCheckoutClick = onCheckoutClick,
        )
    }
}
