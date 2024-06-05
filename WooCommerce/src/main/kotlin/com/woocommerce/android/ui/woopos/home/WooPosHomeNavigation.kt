package com.woocommerce.android.ui.woopos.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val HOME_ROUTE = "cart-checkout"

fun NavController.navigateToCartCheckoutScreen() {
    navigate(HOME_ROUTE)
}

fun NavGraphBuilder.cartCheckoutScreen() {
    composable(HOME_ROUTE) {
        WooPosHomeScreen()
    }
}
