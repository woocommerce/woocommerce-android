package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order

fun Order.adjustProductQuantity(productId: Long, quantityToAdd: Int): Order {
    val items = items.toMutableList()
    val index = items.indexOfFirst { it.uniqueId == productId }
    if (index == -1) error("Couldn't find the product with id $productId")
    items[index] = with(items[index]) {
        val newQuantity = quantity + quantityToAdd
        copy(
            quantity = newQuantity,
            subtotal = price.multiply(newQuantity.toBigDecimal()),
            total = price.multiply(newQuantity.toBigDecimal())
        )
    }
    return updateItems(items)
}

fun Order.updateItems(items: List<Order.Item>): Order = copy(
    items = items
)
