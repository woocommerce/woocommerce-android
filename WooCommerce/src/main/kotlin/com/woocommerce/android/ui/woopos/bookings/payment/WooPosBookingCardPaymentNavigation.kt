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
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val BOOKING_CARD_PAYMENT_ORDER_ID_KEY = "orderId"
const val BOOKING_CARD_PAYMENT_ROUTE =
    "$BOOKINGS_ROUTE/card_payment/{$BOOKING_CARD_PAYMENT_ORDER_ID_KEY}"

fun NavController.navigateToBookingCardPayment(orderId: Long) {
    navigateOnce(
        BOOKING_CARD_PAYMENT_ROUTE.replace(
            "{$BOOKING_CARD_PAYMENT_ORDER_ID_KEY}",
            orderId.toString()
        )
    )
}

fun NavGraphBuilder.bookingCardPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKING_CARD_PAYMENT_ROUTE,
        arguments = listOf(
            navArgument(BOOKING_CARD_PAYMENT_ORDER_ID_KEY) { type = NavType.LongType }
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
    ) {
        WooPosBookingCardPaymentScreen(onNavigationEvent = onNavigationEvent)
    }
}
