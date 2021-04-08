package com.woocommerce.android.extensions

import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundListItem
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

fun List<RefundListItem>.calculateTotals(): Pair<BigDecimal, BigDecimal> {
    var taxes = BigDecimal.ZERO
    var subtotal = BigDecimal.ZERO
    this.forEach { item ->
        val quantity = item.quantity
        subtotal += quantity.times(item.orderItem.price)

        val singleItemTax = item.orderItem.totalTax.divide(
                item.orderItem.quantity,
                2,
                HALF_UP
        )
        taxes += quantity.times(singleItemTax)
    }
    return Pair(subtotal, taxes)
}
