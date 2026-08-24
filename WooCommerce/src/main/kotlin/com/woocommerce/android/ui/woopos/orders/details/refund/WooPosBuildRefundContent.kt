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
        preservedSelection: Set<String>? = null,
    ): WooPosRefundState.Content {
        val allItemIds = refundableItems.map { it.uniqueId }.toSet()
        // A preserved selection that no longer matches anything leaves the selection empty rather
        // than falling back to every remaining item: the cashier picks again instead of confirming
        // items they never chose.
        val selectedItemIds = preservedSelection
            ?.filterTo(mutableSetOf()) { it in allItemIds }
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
