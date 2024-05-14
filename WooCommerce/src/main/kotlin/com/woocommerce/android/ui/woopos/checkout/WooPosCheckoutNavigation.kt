package com.woocommerce.android.ui.woopos.checkout

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal const val CHECKOUT_ROUTE = "checkout"

internal fun NavGraphBuilder.checkoutScreen() {
    composable(CHECKOUT_ROUTE) {
        val viewModel: WooPosCheckoutViewModel = hiltViewModel()
        WooPosCheckoutScreen(viewModel)
    }
}
