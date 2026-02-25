package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosOrderActionsProvider @Inject constructor() {
    internal var isPosRefundsEnabled: () -> Boolean = { FeatureFlag.POS_REFUNDS.isEnabled() }

    fun getAvailableActions(
        order: Order
    ): List<WooPosOrdersState.OrderAction> {
        return buildList {
            if (isPosRefundsEnabled() && order.status == Order.Status.Completed) {
                add(WooPosOrdersState.OrderAction.IssueRefund(order.id))
            }
            if (order.status in PAID_ORDER_STATUSES) {
                add(WooPosOrdersState.OrderAction.EmailReceipt(order.id))
            }
        }
    }

    companion object {
        private val PAID_ORDER_STATUSES = setOf(
            Order.Status.Completed,
            Order.Status.Processing,
            Order.Status.Refunded,
        )
    }
}
