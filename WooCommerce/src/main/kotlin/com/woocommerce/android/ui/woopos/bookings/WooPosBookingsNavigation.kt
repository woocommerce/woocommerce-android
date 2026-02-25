package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.orders.details.refund.REFUND_REASON_RESULT_KEY
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val BOOKINGS_ROUTE = "$HOME_ROUTE/bookings"
const val BOOKING_PAYMENT_FLOW_FINISHED_KEY = "booking_payment_flow_finished"

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
        val refundReasonResult = backStackEntry.savedStateHandle
            .getStateFlow<String?>(REFUND_REASON_RESULT_KEY, null)
            .collectAsState()

        if (refundReasonResult.value != null) {
            backStackEntry.savedStateHandle.remove<String>(REFUND_REASON_RESULT_KEY)
        }

        WooPosBookingsScreen(
            onNavigationEvent = onNavigationEvent,
            backStackEntry = backStackEntry,
            refundReasonResult = refundReasonResult.value,
        )
    }
}
