package com.woocommerce.android.ui.woopos.home.orders

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val ORDER_DETAILS_ROUTE_ORDER_ID_KEY = "orderId"
const val ORDER_DETAILS_ROUTE = "$HOME_ROUTE/order_details/{$ORDER_DETAILS_ROUTE_ORDER_ID_KEY}"

fun NavController.navigateToOrderDetailsScreen(orderId: Long) {
    navigateOnce(ORDER_DETAILS_ROUTE.replace("{$ORDER_DETAILS_ROUTE_ORDER_ID_KEY}", orderId.toString()))
}

fun NavGraphBuilder.orderDetailsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = ORDER_DETAILS_ROUTE,
        arguments = listOf(
            navArgument(ORDER_DETAILS_ROUTE_ORDER_ID_KEY) { type = NavType.LongType }
        ),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) { backStackEntry ->
        val orderId = backStackEntry.arguments?.getLong(ORDER_DETAILS_ROUTE_ORDER_ID_KEY) ?: 0L
        WooPosOrderDetailsScreen(
            orderId = orderId,
            onNavigationEvent = onNavigationEvent,
        )
    }
}
