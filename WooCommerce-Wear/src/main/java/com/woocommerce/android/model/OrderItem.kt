package com.woocommerce.android.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

fun String.fromJson(gson: Gson): List<OrderItem> {
    val responseType = object : TypeToken<List<LineItem>>() {}.type
    val items = gson.fromJson(this, responseType) as? List<LineItem> ?: emptyList()
    return items.map { it.toAppModel() }
}
