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
import java.net.URLDecoder
import java.net.URLEncoder

const val PAYMENT_SUCCESS_ORDER_ID_KEY = "orderId"
const val PAYMENT_SUCCESS_ORDER_TOTAL_TEXT_KEY = "orderTotalText"
const val PAYMENT_SUCCESS_SOURCE_KEY = "source"
const val PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY = "receiptSentMessage"

const val PAYMENT_SUCCESS_ROUTE =
    "$HOME_ROUTE/payment_success/{$PAYMENT_SUCCESS_ORDER_ID_KEY}" +
        "/{$PAYMENT_SUCCESS_ORDER_TOTAL_TEXT_KEY}" +
        "/{$PAYMENT_SUCCESS_SOURCE_KEY}" +
        "?$PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY={$PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY}"

enum class PaymentSuccessSource {
    CARD_CHECKOUT,
    CARD_BOOKINGS,
    CASH_BOOKINGS,
}

fun NavController.navigateToPaymentSuccessScreen(
    orderId: Long,
    orderTotalText: String,
    source: PaymentSuccessSource,
    receiptSentMessage: String? = null,
) {
    val route = PAYMENT_SUCCESS_ROUTE
        .replace("{$PAYMENT_SUCCESS_ORDER_ID_KEY}", orderId.toString())
        .replace("{$PAYMENT_SUCCESS_ORDER_TOTAL_TEXT_KEY}", orderTotalText.encodeForRoute())
        .replace("{$PAYMENT_SUCCESS_SOURCE_KEY}", source.name)
        .replace(
            "{$PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY}",
            receiptSentMessage?.encodeForRoute().orEmpty()
        )
    navigateOnce(route)
}

fun NavGraphBuilder.paymentSuccessScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = PAYMENT_SUCCESS_ROUTE,
        arguments = listOf(
            navArgument(PAYMENT_SUCCESS_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(PAYMENT_SUCCESS_ORDER_TOTAL_TEXT_KEY) { type = NavType.StringType },
            navArgument(PAYMENT_SUCCESS_SOURCE_KEY) { type = NavType.StringType },
            navArgument(PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
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
    ) { backStackEntry ->
        val orderId = backStackEntry.arguments?.getLong(PAYMENT_SUCCESS_ORDER_ID_KEY) ?: 0L
        val orderTotalText = backStackEntry.arguments?.getString(PAYMENT_SUCCESS_ORDER_TOTAL_TEXT_KEY)
            ?.let { URLDecoder.decode(it, "UTF-8") }
            .orEmpty()
        val source = backStackEntry.arguments?.getString(PAYMENT_SUCCESS_SOURCE_KEY)
            ?.let { runCatching { PaymentSuccessSource.valueOf(it) }.getOrNull() }
            ?: PaymentSuccessSource.CARD_CHECKOUT
        val receiptSentMessage = backStackEntry.arguments?.getString(PAYMENT_SUCCESS_RECEIPT_SENT_MESSAGE_KEY)
            ?.let { URLDecoder.decode(it, "UTF-8") }

        WooPosPaymentSuccessScreen(
            orderId = orderId,
            orderTotalText = orderTotalText,
            source = source,
            receiptSentMessage = receiptSentMessage,
            onNavigationEvent = onNavigationEvent,
        )
    }
}

private fun String.encodeForRoute(): String =
    URLEncoder.encode(this, "UTF-8")
