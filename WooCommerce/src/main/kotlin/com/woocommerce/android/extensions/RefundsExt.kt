package com.woocommerce.android.extensions

import com.woocommerce.android.ui.payments.refunds.RefundProductListAdapter.ProductRefundListItem
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

fun List<ProductRefundListItem>.calculateTotals(): Pair<BigDecimal, BigDecimal> {
    var taxes = BigDecimal.ZERO
    var subtotal = BigDecimal.ZERO
    this.forEach { item ->
        val (itemSubtotal, itemTaxes) = item.calculateTotal()
        subtotal += itemSubtotal
        taxes += itemTaxes
    }
    return Pair(subtotal, taxes)
}

fun ProductRefundListItem.calculateTotal(): Pair<BigDecimal, BigDecimal> {
    val quantity = quantity.toBigDecimal()
    val subtotal = quantity.times(orderItem.price)

    val singleItemTax = orderItem.totalTax.divide(orderItem.quantity.toBigDecimal(), 2, HALF_UP)
    val taxes = quantity.times(singleItemTax)
    return Pair(subtotal, taxes)
}
