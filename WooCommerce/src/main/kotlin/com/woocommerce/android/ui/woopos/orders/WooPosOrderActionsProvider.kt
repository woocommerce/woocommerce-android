package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosGetRefundableItems
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosOrderActionsProvider @Inject constructor(
    private val getRefundableItems: WooPosGetRefundableItems,
    private val featureFlagRepository: FeatureFlagRepository
) {

    suspend fun getAvailableActions(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<WooPosOrdersState.OrderAction> {
        val isPosRefundsEnabled = featureFlagRepository.isEnabled(FeatureFlag.POS_REFUNDS)
        return buildList {
            if (isPosRefundsEnabled && hasRefundableItems(order, refundResult)) {
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
