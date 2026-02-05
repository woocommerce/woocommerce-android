package com.woocommerce.android.ui.woopos.bookings.payment

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.bookings.BOOKINGS_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import java.net.URLDecoder
import java.net.URLEncoder

const val BOOKING_PAYMENT_RESULT_KEY = "booking_payment_completed"

private const val BOOKING_PAYMENT_SUCCESS_ORDER_ID_KEY = "orderId"
private const val BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY = "amount"
private const val BOOKING_PAYMENT_SUCCESS_ROUTE =
    "$BOOKINGS_ROUTE/payment_success/{$BOOKING_PAYMENT_SUCCESS_ORDER_ID_KEY}" +
        "?$BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY={$BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY}"

fun NavController.navigateToBookingPaymentSuccess(orderId: Long, amountLabel: String) {
    val encodedAmount = URLEncoder.encode(amountLabel, "UTF-8")
    val route = "$BOOKINGS_ROUTE/payment_success/$orderId" +
        "?$BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY=$encodedAmount"
    navigate(route) {
        popUpTo(BOOKINGS_ROUTE)
    }
}

fun NavGraphBuilder.bookingPaymentSuccessScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKING_PAYMENT_SUCCESS_ROUTE,
        arguments = listOf(
            navArgument(BOOKING_PAYMENT_SUCCESS_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) { backStackEntry ->
        val orderId = backStackEntry.arguments?.getLong(BOOKING_PAYMENT_SUCCESS_ORDER_ID_KEY) ?: 0L
        val amountLabel = backStackEntry.arguments?.getString(BOOKING_PAYMENT_SUCCESS_AMOUNT_KEY)?.let {
            URLDecoder.decode(it, "UTF-8")
        } ?: ""
        WooPosBookingPaymentSuccessScreen(
            orderId = orderId,
            amountLabel = amountLabel,
            onNavigationEvent = onNavigationEvent,
        )
    }
}
