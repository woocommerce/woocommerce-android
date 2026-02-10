package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosOrderActionsProvider @Inject constructor(private val featureFlagRepository: FeatureFlagRepository) {
    suspend fun getAvailableActions(
        order: Order
    ): List<WooPosOrdersState.OrderAction> {
        val isPosRefundsEnabled = featureFlagRepository.isEnabled(FeatureFlag.POS_REFUNDS)
        return buildList {
            if (isPosRefundsEnabled && order.status == Order.Status.Completed) {
                add(WooPosOrdersState.OrderAction.IssueRefund(order.id))
            }
            add(WooPosOrdersState.OrderAction.EmailReceipt(order.id))
        }
    }
}
