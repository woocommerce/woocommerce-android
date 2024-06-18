package com.woocommerce.android.model

import java.math.BigDecimal
import org.wordpress.android.fluxc.model.order.LineItem

data class OrderItem(
    val itemId: Long,
    val name: String,
    val quantity: Float,
    val totalTax: BigDecimal,
    val price: BigDecimal,
    val total: BigDecimal
)

fun LineItem.toAppModel() = OrderItem(
    itemId = id ?: 0L,
    name = name.orEmpty(),
    quantity = quantity ?: 0F,
    totalTax = totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    price = price?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    total = total?.toBigDecimalOrNull() ?: BigDecimal.ZERO
)
