package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosOrderActionsProvider @Inject constructor(
    private val getRefundableItems: WooPosGetRefundableItems,
) {
    fun getAvailableActions(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<WooPosOrdersState.OrderAction> {
        return buildList {
            if (FeatureFlag.POS_REFUNDS.isEnabled() && hasRefundableItems(order, refundResult)) {
                add(WooPosOrdersState.OrderAction.IssueRefund(order.id))
            }
            add(WooPosOrdersState.OrderAction.EmailReceipt(order.id))
        }
    }

    private fun hasRefundableItems(order: Order, refundResult: RefundsFetchResult): Boolean {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> emptyList()
        }
        return getRefundableItems(order, refunds).isNotEmpty()
    }
}
