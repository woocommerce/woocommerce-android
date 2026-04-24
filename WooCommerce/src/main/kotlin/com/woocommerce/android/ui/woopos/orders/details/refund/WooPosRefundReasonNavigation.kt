package com.woocommerce.android.ui.woopos.orders.details.refund

import android.os.Build
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce
import java.net.URLEncoder

const val REFUND_REASON_RESULT_KEY = "refund_reason_result"
const val REFUND_REASON_ROUTE_ORDER_ID_KEY = "orderId"
const val REFUND_REASON_INITIAL_REASON_KEY = "initialReason"
private const val REFUND_REASON_ROUTE =
    "$ORDERS_ROUTE/refund_reason/{$REFUND_REASON_ROUTE_ORDER_ID_KEY}/{$REFUND_REASON_INITIAL_REASON_KEY}"

fun NavController.navigateToRefundReason(orderId: Long, initialReason: String = "") {
    val encodedReason = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        URLEncoder.encode(initialReason, Charsets.UTF_8)
    } else {
        URLEncoder.encode(initialReason, "UTF-8")
    }
    navigateOnce(
        REFUND_REASON_ROUTE
            .replace("{$REFUND_REASON_ROUTE_ORDER_ID_KEY}", orderId.toString())
            .replace("{$REFUND_REASON_INITIAL_REASON_KEY}", encodedReason)
    )
}

fun NavGraphBuilder.refundReasonScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = REFUND_REASON_ROUTE,
        arguments = listOf(
            navArgument(REFUND_REASON_ROUTE_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(REFUND_REASON_INITIAL_REASON_KEY) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) { backStackEntry ->
        val orderId = backStackEntry.arguments?.getLong(REFUND_REASON_ROUTE_ORDER_ID_KEY) ?: 0L

        val initialReason = backStackEntry.arguments?.getString(REFUND_REASON_INITIAL_REASON_KEY)
            ?.let { encoded ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    java.net.URLDecoder.decode(encoded, Charsets.UTF_8)
                } else {
                    java.net.URLDecoder.decode(encoded, "UTF-8")
                }
            } ?: ""

        WooPosRefundReasonScreen(
            orderId = orderId,
            initialReason = initialReason,
            onNavigationEvent = onNavigationEvent,
        )
    }
}
