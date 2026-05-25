package com.woocommerce.android.ui.woopos.paymentsuccess

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

const val PAYMENT_SUCCESS_ORDER_ID_KEY = "orderId"
const val PAYMENT_SUCCESS_SOURCE_KEY = "source"

const val PAYMENT_SUCCESS_ROUTE =
    "$HOME_ROUTE/payment_success/{$PAYMENT_SUCCESS_ORDER_ID_KEY}" +
        "/{$PAYMENT_SUCCESS_SOURCE_KEY}"

enum class PaymentSuccessSource {
    CARD_CHECKOUT,
    CARD_BOOKINGS,
    CASH_BOOKINGS,
    SCAN_TO_PAY,
    MARK_ORDER_AS_PAID,
}

fun NavController.navigateToPaymentSuccessScreen(
    orderId: Long,
    source: PaymentSuccessSource,
) {
    val route = PAYMENT_SUCCESS_ROUTE
        .replace("{$PAYMENT_SUCCESS_ORDER_ID_KEY}", orderId.toString())
        .replace("{$PAYMENT_SUCCESS_SOURCE_KEY}", source.name)
    navigateOnce(route)
}

fun NavGraphBuilder.paymentSuccessScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = PAYMENT_SUCCESS_ROUTE,
        arguments = listOf(
            navArgument(PAYMENT_SUCCESS_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(PAYMENT_SUCCESS_SOURCE_KEY) { type = NavType.StringType },
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
        WooPosPaymentSuccessScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
