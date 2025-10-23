package com.woocommerce.android.ui.woopos.orders

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.emailreceipt.EMAIL_RECEIPT_SENT
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val ORDERS_ROUTE = "$HOME_ROUTE/orders"

fun NavController.navigateToOrdersScreen() {
    navigateOnce(ORDERS_ROUTE)
}

fun NavGraphBuilder.ordersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = ORDERS_ROUTE,
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
        val navigatedFromEmailReceiptSent = backStackEntry.savedStateHandle
            .getStateFlow(EMAIL_RECEIPT_SENT, false)
            .collectAsState()

        backStackEntry.savedStateHandle.remove<Boolean>(EMAIL_RECEIPT_SENT)

        WooPosOrdersScreen(
            onNavigationEvent = onNavigationEvent,
            navigatedFromEmailReceiptSent = navigatedFromEmailReceiptSent.value
        )
    }
}
