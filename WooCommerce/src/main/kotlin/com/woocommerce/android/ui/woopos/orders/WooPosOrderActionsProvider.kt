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
            add(WooPosOrdersState.OrderAction.EmailReceipt(order.id))
        }
    }
}
