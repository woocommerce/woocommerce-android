package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosGetRefundableItems
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosBookingActionsProvider @Inject constructor(
    private val getRefundableItems: WooPosGetRefundableItems,
) {
    internal var isPosRefundsEnabled: () -> Boolean = { FeatureFlag.POS_REFUNDS.isEnabled() }

    fun getAvailableActions(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<WooPosBookingsState.BookingAction> {
        return buildList {
            if (isPosRefundsEnabled() && hasRefundableItems(order, refundResult)) {
                add(WooPosBookingsState.BookingAction.IssueRefund(order.id))
            }
            add(WooPosBookingsState.BookingAction.EmailReceipt(order.id))
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
