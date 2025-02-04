package com.woocommerce.android.extensions

import com.woocommerce.android.ui.payments.refunds.RefundProductListAdapter.ProductRefundListItem
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

fun List<ProductRefundListItem>.calculateTotals(): Pair<BigDecimal, BigDecimal> {
    var taxes = BigDecimal.ZERO
    var subtotal = BigDecimal.ZERO
    this.forEach { item ->
        subtotal += item.calculateTotalSubtotal()
        taxes += item.calculateTotalTaxes()
    }
    return Pair(subtotal, taxes)
}

fun ProductRefundListItem.calculateTotalSubtotal(): BigDecimal {
    val quantity = quantity.toBigDecimal()
    return quantity.times(orderItem.price)
}

fun ProductRefundListItem.calculateTotalTaxes(): BigDecimal {
    val quantity = quantity.toBigDecimal()

    val singleItemTax = orderItem.totalTax.divide(orderItem.quantity.toBigDecimal(), 2, HALF_UP)
    return quantity.times(singleItemTax)
}
