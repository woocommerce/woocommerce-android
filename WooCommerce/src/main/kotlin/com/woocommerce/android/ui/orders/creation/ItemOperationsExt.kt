package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.FeatureFlag

fun Order.adjustProductQuantity(productId: Long, quantityToAdd: Int): Order {
    val items = items.toMutableList()
    val index = items.indexOfFirst { it.uniqueId == productId }
    if (index == -1) error("Couldn't find the product with id $productId")
    items[index] = with(items[index]) {
        val newQuantity = quantity + quantityToAdd
        copy(
            quantity = newQuantity,
            subtotal = pricePreDiscount.multiply(newQuantity.toBigDecimal()),
            total = price.multiply(newQuantity.toBigDecimal())
        )
    }
    return updateItems(items)
}

fun Order.updateItems(items: List<Order.Item>): Order = copy(
    items = items,
    // Handle local total calculation only on M1
    productsTotal = if (FeatureFlag.ORDER_CREATION_M2.isEnabled()) productsTotal else items.sumOf { it.subtotal },
    total = if (FeatureFlag.ORDER_CREATION_M2.isEnabled()) total else items.sumOf { it.subtotal }
)
