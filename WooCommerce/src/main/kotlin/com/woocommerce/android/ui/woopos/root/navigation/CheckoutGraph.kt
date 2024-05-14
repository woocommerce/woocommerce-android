package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.cart.CART_ROUTE
import com.woocommerce.android.ui.woopos.cart.cartScreen
import com.woocommerce.android.ui.woopos.checkout.checkoutScreen

const val CHECKOUT_GRAPH_ROUTE = "checkout-graph"

fun NavController.navigateToCheckoutGraph() {
    navigate(CHECKOUT_GRAPH_ROUTE)
}

fun NavGraphBuilder.checkoutGraph(
    navController: NavController
) {
    navigation(
        startDestination = CART_ROUTE,
        route = CHECKOUT_GRAPH_ROUTE,
    ) {
        cartScreen()
        checkoutScreen()
    }
}
