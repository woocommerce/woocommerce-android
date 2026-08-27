package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import javax.inject.Inject

class WooPosBuildRefundContent @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
) {
    operator fun invoke(
        order: Order,
        refundableItems: List<WooPosRefundableItem>,
        paymentMethod: String,
        preservedSelection: WooPosRefundSelectionSnapshot? = null,
    ): WooPosRefundState.Content {
        val allItemIds = refundableItems.map { it.uniqueId }.toSet()
        // An empty restore stays empty; only the absence of a snapshot selects everything.
        val selectedItemIds = preservedSelection
            ?.let { restoreSelection(refundableItems, it) }
            ?: allItemIds
        val zero = PriceUtils.formatCurrency(BigDecimal.ZERO, order.currency, currencyFormatter)

        return WooPosRefundState.Content(
            orderId = order.id,
            orderNumber = "#${order.number}",
            currency = order.currency,
            refundableItems = refundableItems,
            selectedItemIds = selectedItemIds,
            allItemsSelected = selectedItemIds.containsAll(allItemIds),
            itemsCount = refundableItems.count { it.uniqueId in selectedItemIds },
            subtotal = BigDecimal.ZERO,
            taxes = BigDecimal.ZERO,
            total = BigDecimal.ZERO,
            formattedSubtotal = zero,
            formattedTaxes = zero,
            formattedTotal = zero,
            paymentMethod = paymentMethod,
            step = WooPosRefundState.Content.RefundStep.SelectItems
        )
    }

    /**
     * Re-applies [snapshot] to the reloaded rows: the first *n* rows of each line item in list
     * order, capped at what is left of it, plus the lump sums that were selected. List order is
     * [WooPosRefundableItem.rowIndex] order because [WooPosGetRefundableItems] emits the units of a
     * line as `(0 until maxQuantity)`.
     */
    private fun restoreSelection(
        refundableItems: List<WooPosRefundableItem>,
        snapshot: WooPosRefundSelectionSnapshot,
    ): Set<String> {
        val remainingUnits = snapshot.unitCountsByOrderItemId.toMutableMap()

        return refundableItems.mapNotNullTo(mutableSetOf()) { item ->
            if (item.isLumpSum) {
                item.uniqueId.takeIf { item.orderItemId in snapshot.selectedLumpSumIds }
            } else {
                val left = remainingUnits[item.orderItemId] ?: 0
                if (left > 0) {
                    remainingUnits[item.orderItemId] = left - 1
                    item.uniqueId
                } else {
                    null
                }
            }
        }
    }
}
