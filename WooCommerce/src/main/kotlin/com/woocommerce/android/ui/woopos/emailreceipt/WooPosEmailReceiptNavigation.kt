package com.woocommerce.android.ui.woopos.emailreceipt

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

const val EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY = "orderId"
private const val EMAIL_RECEIPT_ROUTE = "$HOME_ROUTE/email_receipt/{$EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY}"

fun NavController.navigateToEmailReceipt(orderId: Long) {
    navigateOnce(EMAIL_RECEIPT_ROUTE.replace("{$EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY}", orderId.toString()))
}

fun NavGraphBuilder.emailReceiptScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = EMAIL_RECEIPT_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY) { type = NavType.LongType }
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
    ) { backStackEntry ->
        WooPosEmailReceiptScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
