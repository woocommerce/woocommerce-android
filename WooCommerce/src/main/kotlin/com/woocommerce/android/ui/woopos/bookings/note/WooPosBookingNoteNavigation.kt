package com.woocommerce.android.ui.woopos.bookings.note

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

const val BOOKING_NOTE_RESULT_KEY = "booking_note_saved"
const val BOOKING_NOTE_ROUTE_BOOKING_ID_KEY = "bookingId"
private const val BOOKING_NOTE_ROUTE =
    "$BOOKINGS_ROUTE/note/{$BOOKING_NOTE_ROUTE_BOOKING_ID_KEY}"

fun NavController.navigateToBookingNoteScreen(bookingId: Long) {
    navigateOnce(
        BOOKING_NOTE_ROUTE
            .replace("{$BOOKING_NOTE_ROUTE_BOOKING_ID_KEY}", bookingId.toString())
    )
}

fun NavGraphBuilder.bookingNoteScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKING_NOTE_ROUTE,
        arguments = listOf(
            navArgument(BOOKING_NOTE_ROUTE_BOOKING_ID_KEY) { type = NavType.LongType }
        ),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) {
        WooPosBookingNoteScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
