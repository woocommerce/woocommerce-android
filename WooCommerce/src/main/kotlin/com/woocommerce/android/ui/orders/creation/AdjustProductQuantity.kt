package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductType
import javax.inject.Inject

class AdjustProductQuantity @Inject constructor() {
    operator fun invoke(order: Order, product: OrderCreationProduct, quantityToAdd: Int): Order {
        return when (product.productInfo.productType) {
            ProductType.BUNDLE -> adjustBundleQuantity(order, product, quantityToAdd)
            else -> adjustQuantity(order, product.item.itemId, quantityToAdd)
        }
    }

    operator fun invoke(order: Order, itemId: Long, quantityToAdd: Int) = adjustQuantity(order, itemId, quantityToAdd)

    private fun adjustBundleQuantity(order: Order, product: OrderCreationProduct, quantityToAdd: Int): Order {
        return (product as? OrderCreationProduct.GroupedProductItemWithRules)?.let { groupedProduct ->
            val items = order.items.associateBy { it.itemId }.toMutableMap()
            items[product.item.itemId]?.run {
                items[product.item.itemId] = copy(quantity = 0f)
                val newQuantity = quantity + quantityToAdd
                val discountAmount = subtotal - total
                val newSubtotal = pricePreDiscount.multiply(newQuantity.toBigDecimal())
                items[0L] = copy(
                    itemId = 0L,
                    quantity = newQuantity,
                    subtotal = newSubtotal,
                    total = newSubtotal - discountAmount,
                    configuration = groupedProduct.configuration
                )
            }
            for (child in product.children) {
                val updatedItem = items[child.item.itemId]?.copy(quantity = 0f) ?: continue
                items[child.item.itemId] = updatedItem
            }
            order.copy(items = items.values.toList())
        } ?: run { adjustQuantity(order, product.item.itemId, quantityToAdd) }
    }

    private fun adjustQuantity(order: Order, itemId: Long, quantityToAdd: Int): Order {
        val items = order.items.toMutableList()
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
        return order.copy(items = items)
    }
}
