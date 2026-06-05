package com.woocommerce.android.ui.woopos.markorderascomplete

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

const val MARK_ORDER_AS_COMPLETE_ROUTE =
    "$HOME_ROUTE/mark_order_as_complete/{$MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY}"

fun NavController.navigateToMarkOrderAsCompleteScreen(orderId: Long) {
    navigateOnce(
        MARK_ORDER_AS_COMPLETE_ROUTE
            .replace("{$MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY}", orderId.toString())
    )
}

fun NavGraphBuilder.markOrderAsCompleteScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    composable(
        route = MARK_ORDER_AS_COMPLETE_ROUTE,
        arguments = listOf(
            navArgument(MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
        ),
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
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) {
        WooPosMarkOrderAsCompleteScreen(onNavigationEvent = onNavigationEvent)
    }
}
