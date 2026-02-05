package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.bookings.payment.BOOKING_PAYMENT_RESULT_KEY
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val BOOKINGS_ROUTE = "$HOME_ROUTE/bookings"

fun NavController.navigateToBookingsScreen() {
    navigateOnce(BOOKINGS_ROUTE)
}

fun NavGraphBuilder.bookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKINGS_ROUTE,
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
        val paymentCompleted by backStackEntry.savedStateHandle
            .getStateFlow(BOOKING_PAYMENT_RESULT_KEY, false)
            .collectAsState()

        WooPosBookingsScreen(
            onNavigationEvent = onNavigationEvent,
            paymentCompleted = paymentCompleted,
            onPaymentResultConsumed = {
                backStackEntry.savedStateHandle[BOOKING_PAYMENT_RESULT_KEY] = false
            },
        )
    }
}
