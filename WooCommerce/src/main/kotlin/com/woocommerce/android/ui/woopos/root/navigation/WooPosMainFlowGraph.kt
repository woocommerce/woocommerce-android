package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.home.cartCheckoutScreen

const val MAIN_GRAPH_ROUTE = "main-graph"

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.checkoutGraph(
    navController: NavController,
    onPosExitClicked: () -> Unit
) {
    navigation(
        startDestination = HOME_ROUTE,
        route = MAIN_GRAPH_ROUTE,
    ) {
        cartCheckoutScreen(onPosExitClicked = onPosExitClicked)
    }
}
