package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order

fun Order.adjustProductQuantity(itemId: Long, quantityToAdd: Int): Order {
    val items = items.toMutableList()
    val index = items.indexOfFirst { it.itemId == itemId }
    if (index == -1) error("Couldn't find the product with id $itemId")
    items[index] = with(items[index]) {
        val newQuantity = quantity + quantityToAdd
        val discountAmount = subtotal - total
        val newSubtotal = pricePreDiscount.multiply(newQuantity.toBigDecimal())
        copy(
            quantity = newQuantity,
            subtotal = newSubtotal,
            total = newSubtotal - discountAmount
        )
    }
    return updateItems(items)
}

fun Order.updateItems(items: List<Order.Item>): Order = copy(
    items = items,
    productsTotal = productsTotal,
    total = total
)

fun Order.updateItem(item: Order.Item): Order = copy(
    items = items.map { if (it.itemId == item.itemId) item else it },
)
