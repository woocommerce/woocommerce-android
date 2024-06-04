package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val CART_CHECKOUT_ROUTE = "cart-checkout"

fun NavController.navigateToCartCheckoutScreen() {
    navigate(CART_CHECKOUT_ROUTE)
}

fun NavGraphBuilder.cartCheckoutScreen() {
    composable(CART_CHECKOUT_ROUTE) {
        val viewModel: WooPosCartCheckoutViewModel = hiltViewModel()

        WooPosCartCheckoutScreen(viewModel = viewModel)
    }
}
