package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Builds the item-selection content of the refund flow. Totals start at zero: they are resolved
 * when the cashier continues.
 */
class WooPosBuildRefundContent @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
) {
    /**
     * @param preservedSelection selection kept from a reload. Ids that are no longer refundable are
     * dropped. If none are left, all items are selected.
     */
    operator fun invoke(
        order: Order,
        refundableItems: List<WooPosRefundableItem>,
        paymentMethod: String,
        preservedSelection: Set<String>? = null,
    ): WooPosRefundState.Content {
        val allItemIds = refundableItems.map { it.uniqueId }.toSet()
        val selectedItemIds = preservedSelection
            ?.filterTo(mutableSetOf()) { it in allItemIds }
            ?.takeIf { it.isNotEmpty() }
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
}
