package com.woocommerce.android.ui.woopos.cashpayment

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.home.IsHomePaymentCompletedViaCash
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val CASH_ROUTE_ORDER_ID_KEY = "orderId"
const val CASH_ROUTE_SOURCE_KEY = "source"
const val CASH_ROUTE =
    "$HOME_ROUTE/cash_payment/{$CASH_ROUTE_ORDER_ID_KEY}?$CASH_ROUTE_SOURCE_KEY={$CASH_ROUTE_SOURCE_KEY}"

enum class CashPaymentSource {
    CHECKOUT,
    BOOKINGS,
}

fun NavController.navigateToCashPaymentScreen(
    orderId: Long,
    source: CashPaymentSource = CashPaymentSource.CHECKOUT,
) {
    navigateOnce(
        CASH_ROUTE
            .replace("{$CASH_ROUTE_ORDER_ID_KEY}", orderId.toString())
            .replace("{$CASH_ROUTE_SOURCE_KEY}", source.name)
    )
}

fun NavGraphBuilder.cashPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = CASH_ROUTE,
        arguments = listOf(
            navArgument(CASH_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(CASH_ROUTE_SOURCE_KEY) {
                type = NavType.StringType
                defaultValue = CashPaymentSource.CHECKOUT.name
            }
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
            if (targetState.IsHomePaymentCompletedViaCash) {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                )
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                )
            }
        },
    ) { backStackEntry ->
        WooPosCashPaymentScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
