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
     * Re-applies [snapshot] to the reloaded rows: the first *n* units of each line item, capped at
     * what is left of it, plus the lump sums that were selected. Units of the same line item are
     * interchangeable — only how many are selected reaches the server.
     *
     * A selection that no longer matches anything leaves the selection empty rather than falling
     * back to every remaining item: the cashier picks again instead of confirming items they never
     * chose.
     */
    private fun restoreSelection(
        refundableItems: List<WooPosRefundableItem>,
        snapshot: WooPosRefundSelectionSnapshot,
    ): Set<String> {
        val remainingUnits = snapshot.unitCountsByItemId.toMutableMap()

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
