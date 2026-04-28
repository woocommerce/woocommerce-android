package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val ISSUE_REFUND_DISMISSED_KEY = "issue_refund_dismissed"
private const val ISSUE_REFUND_ORDER_ID_KEY = "orderId"
private const val ISSUE_REFUND_DISABLE_PARTIAL_KEY = "disablePartialRefund"
private const val ISSUE_REFUND_ROUTE =
    "$ORDERS_ROUTE/issue_refund/{$ISSUE_REFUND_ORDER_ID_KEY}?$ISSUE_REFUND_DISABLE_PARTIAL_KEY={$ISSUE_REFUND_DISABLE_PARTIAL_KEY}"

fun NavController.navigateToIssueRefundScreen(orderId: Long, disablePartialRefund: Boolean = false) {
    navigateOnce(
        ISSUE_REFUND_ROUTE
            .replace("{$ISSUE_REFUND_ORDER_ID_KEY}", orderId.toString())
            .replace("{$ISSUE_REFUND_DISABLE_PARTIAL_KEY}", disablePartialRefund.toString())
    )
}

fun NavGraphBuilder.issueRefundScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = ISSUE_REFUND_ROUTE,
        arguments = listOf(
            navArgument(ISSUE_REFUND_ORDER_ID_KEY) { type = NavType.LongType },
            navArgument(ISSUE_REFUND_DISABLE_PARTIAL_KEY) {
                type = NavType.BoolType
                defaultValue = false
            }
        ),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) { backStackEntry ->
        val orderId = checkNotNull(backStackEntry.arguments?.getLong(ISSUE_REFUND_ORDER_ID_KEY)) {
            "orderId argument is required for issue refund screen"
        }
        val disablePartialRefund =
            backStackEntry.arguments?.getBoolean(ISSUE_REFUND_DISABLE_PARTIAL_KEY) ?: false

        val refundReasonResult = backStackEntry.savedStateHandle
            .getStateFlow<String?>(REFUND_REASON_RESULT_KEY, null)
            .collectAsState()

        LaunchedEffect(refundReasonResult.value) {
            if (refundReasonResult.value != null) {
                backStackEntry.savedStateHandle.remove<String>(REFUND_REASON_RESULT_KEY)
            }
        }

        WooPosIssueRefundScreen(
            orderId = orderId,
            onNavigationEvent = onNavigationEvent,
            refundReasonUpdate = refundReasonResult.value,
            disablePartialRefund = disablePartialRefund,
        )
    }
}
