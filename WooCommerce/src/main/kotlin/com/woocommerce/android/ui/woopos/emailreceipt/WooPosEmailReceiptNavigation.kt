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

const val EMAIL_RECEIPT_SENT = "email_receipt_sent"
const val EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY = "orderId"
const val EMAIL_RECEIPT_ROUTE_ALREADY_SENT_KEY = "receiptAlreadySent"
private const val EMAIL_RECEIPT_ROUTE =
    "$HOME_ROUTE/email_receipt/{$EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY}?$EMAIL_RECEIPT_ROUTE_ALREADY_SENT_KEY=" +
        "{$EMAIL_RECEIPT_ROUTE_ALREADY_SENT_KEY}"

fun NavController.navigateToEmailReceipt(orderId: Long, receiptAlreadySent: Boolean = false) {
    val route = EMAIL_RECEIPT_ROUTE
        .replace("{$EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY}", orderId.toString())
        .replace("{$EMAIL_RECEIPT_ROUTE_ALREADY_SENT_KEY}", receiptAlreadySent.toString())
    navigateOnce(route)
}

fun NavGraphBuilder.emailReceiptScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = EMAIL_RECEIPT_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(EMAIL_RECEIPT_ROUTE_ALREADY_SENT_KEY) {
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
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) { backStackEntry ->
        WooPosEmailReceiptScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }
}
