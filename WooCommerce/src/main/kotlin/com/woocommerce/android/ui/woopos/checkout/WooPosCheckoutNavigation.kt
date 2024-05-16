package com.woocommerce.android.ui.woopos.checkout

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val CHECKOUT_ROUTE = "checkout"

fun NavController.navigateToCheckoutScreen() {
    navigate(CHECKOUT_ROUTE)
}

fun NavGraphBuilder.checkoutScreen(
    onBackClick: () -> Unit
) {
    composable(CHECKOUT_ROUTE) {
        val viewModel: WooPosCheckoutViewModel = hiltViewModel()

        WooPosCheckoutScreen(
            viewModel = viewModel,
            onBackClick = onBackClick
        )
    }
}
