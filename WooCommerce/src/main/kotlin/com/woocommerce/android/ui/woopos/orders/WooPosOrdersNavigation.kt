package com.woocommerce.android.ui.woopos.orders

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val ORDERS_ROUTE_ORDER_ID_KEY = "orderId"
const val ORDERS_ROUTE = "$HOME_ROUTE/orders"
private const val ORDER_DETAILS_ROUTE = "$ORDERS_ROUTE/{$ORDERS_ROUTE_ORDER_ID_KEY}"

fun NavController.navigateToOrdersScreen() {
    navigateOnce(ORDERS_ROUTE)
}

fun NavController.navigateToOrderDetailsScreen(orderId: Long) {
    navigateOnce("$ORDERS_ROUTE/$orderId")
}

fun NavGraphBuilder.ordersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    ordersComposable(route = ORDERS_ROUTE, onNavigationEvent = onNavigationEvent)
    ordersComposable(
        route = ORDER_DETAILS_ROUTE,
        arguments = listOf(
            navArgument(ORDERS_ROUTE_ORDER_ID_KEY) { type = NavType.LongType }
        ),
        onNavigationEvent = onNavigationEvent,
    )
}

private fun NavGraphBuilder.ordersComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) { backStackEntry ->
        WooPosOrdersScreen(
            onNavigationEvent = onNavigationEvent,
            backStackEntry = backStackEntry,
        )
    }
}
