package com.woocommerce.android.ui.woopos.root.navigation

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.cart.CART_ROUTE
import com.woocommerce.android.ui.woopos.cart.cartScreen
import com.woocommerce.android.ui.woopos.checkout.checkoutScreen
import com.woocommerce.android.ui.woopos.checkout.navigateToCheckoutScreen

const val MAIN_GRAPH_ROUTE = "main-graph"

fun NavGraphBuilder.checkoutGraph(
    navController: NavController,
    onConnectToCardReaderClicked: (Context) -> Unit,
    collectPaymentWithCardReader: (Context) -> Unit,
) {
    navigation(
        startDestination = CART_ROUTE,
        route = MAIN_GRAPH_ROUTE,
    ) {
        cartScreen(
            onCheckoutClick = navController::navigateToCheckoutScreen,
            onConnectToCardReaderClicked = { onConnectToCardReaderClicked(navController.context) },
            onCollectPaymentWithCardReader = { collectPaymentWithCardReader(navController.context) },
        )
        checkoutScreen(
            onBackClick = navController::popBackStack
        )
    }
}
