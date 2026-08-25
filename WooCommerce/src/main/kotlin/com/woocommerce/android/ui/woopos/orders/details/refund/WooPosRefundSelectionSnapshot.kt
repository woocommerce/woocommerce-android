package com.woocommerce.android.ui.woopos.orders.details.refund

/**
 * The cashier's item selection in a form that survives a reload of the refundable items.
 *
 * Unit rows have no stable identity: [WooPosRefundableItem.rowIndex] is the unit's position within
 * what is still refundable, so it is renumbered whenever the line gets shorter. A reload only
 * happens because the order changed, which is exactly when those positions move. Refunds are
 * submitted as a quantity per line item anyway (see [WooPosBuildRefundLineItems]), so the selection
 * is preserved as counts instead.
 *
 * Line items and lump sums are kept apart so a fee line id cannot be read as a line item id.
 */
data class WooPosRefundSelectionSnapshot(
    val unitCountsByItemId: Map<Long, Int>,
    val selectedLumpSumIds: Set<Long>,
) {
    companion object {
        fun of(
            refundableItems: List<WooPosRefundableItem>,
            selectedItemIds: Set<String>,
        ): WooPosRefundSelectionSnapshot {
            val (lumpSums, units) = refundableItems
                .filter { it.uniqueId in selectedItemIds }
                .partition { it.isLumpSum }

            return WooPosRefundSelectionSnapshot(
                unitCountsByItemId = units.groupingBy { it.orderItemId }.eachCount(),
                selectedLumpSumIds = lumpSums.mapTo(mutableSetOf()) { it.orderItemId },
            )
        }
    }
}
