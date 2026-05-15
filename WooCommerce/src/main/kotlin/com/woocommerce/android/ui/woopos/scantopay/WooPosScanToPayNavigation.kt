package com.woocommerce.android.ui.woopos.scantopay

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

const val SCAN_TO_PAY_ROUTE_ORDER_ID_KEY = "orderId"
const val SCAN_TO_PAY_ROUTE = "$HOME_ROUTE/scan_to_pay/{$SCAN_TO_PAY_ROUTE_ORDER_ID_KEY}"

fun NavController.navigateToScanToPayScreen(orderId: Long) {
    navigateOnce(
        SCAN_TO_PAY_ROUTE.replace("{$SCAN_TO_PAY_ROUTE_ORDER_ID_KEY}", orderId.toString())
    )
}

fun NavGraphBuilder.scanToPayScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    composable(
        route = SCAN_TO_PAY_ROUTE,
        arguments = listOf(
            navArgument(SCAN_TO_PAY_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
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
        WooPosScanToPayScreen(onNavigationEvent = onNavigationEvent)
    }
}
