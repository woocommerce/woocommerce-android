package com.woocommerce.android.ui.woopos.cardpayment

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

const val CARD_PAYMENT_ROUTE_ORDER_ID_KEY = "orderId"
const val CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY = "showCashPayment"
const val CARD_PAYMENT_ROUTE =
    "$HOME_ROUTE/card_payment/{$CARD_PAYMENT_ROUTE_ORDER_ID_KEY}" +
        "?$CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY={$CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY}"

fun NavController.navigateToCardPaymentScreen(
    orderId: Long,
    showCashPaymentButton: Boolean = false,
) {
    navigateOnce(
        CARD_PAYMENT_ROUTE
            .replace("{$CARD_PAYMENT_ROUTE_ORDER_ID_KEY}", orderId.toString())
            .replace("{$CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY}", showCashPaymentButton.toString())
    )
}

fun NavGraphBuilder.cardPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = CARD_PAYMENT_ROUTE,
        arguments = listOf(
            navArgument(CARD_PAYMENT_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY) {
                type = NavType.BoolType
                defaultValue = false
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
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) {
        WooPosCardPaymentScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
