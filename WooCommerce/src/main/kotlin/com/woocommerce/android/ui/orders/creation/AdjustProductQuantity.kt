package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductType
import javax.inject.Inject

class AdjustProductQuantity @Inject constructor() {
    operator fun invoke(order: Order, product: OrderCreationProduct, quantityToAdd: Int): Order {
        if (product.item.itemId == Order.Item.EMPTY.itemId) return order
        return when (product.productInfo.productType) {
            ProductType.BUNDLE -> adjustBundleQuantity(order, product, quantityToAdd)
            else -> adjustQuantity(order, product.item.itemId, quantityToAdd)
        }
    }

    operator fun invoke(order: Order, itemId: Long, quantityToAdd: Int): Order {
        return if (itemId == Order.Item.EMPTY.itemId) {
            order
        } else {
            adjustQuantity(order, itemId, quantityToAdd)
        }
    }

    @Suppress("ReturnCount")
    private fun adjustBundleQuantity(order: Order, product: OrderCreationProduct, quantityToAdd: Int): Order {
        val groupedProduct = product as? OrderCreationProduct.GroupedProductItemWithRules
            ?: return adjustQuantity(order, product.item.itemId, quantityToAdd)

        val items = order.items.associateBy { it.itemId }.toMutableMap()
        val parentItem = items[product.item.itemId] ?: return order

        var isProductRemoved = parentItem.quantity + quantityToAdd <= 0
        if (isProductRemoved) {
            items.clearBundleItems(groupedProduct)
        } else {
            items.updateBundleItems(groupedProduct, parentItem, quantityToAdd)
        }

        return order.copy(items = items.values.toList())
    }

    private fun MutableMap<Long, Order.Item>.clearBundleItems(
        product: OrderCreationProduct.GroupedProductItemWithRules
    ) {
        this[product.item.itemId]?.let { this[product.item.itemId] = it.copy(quantity = 0f) }
        product.children.forEach { child ->
            this[child.item.itemId] = this[child.item.itemId]?.copy(quantity = 0f) ?: return@forEach
        }
    }

    private fun MutableMap<Long, Order.Item>.updateBundleItems(
        product: OrderCreationProduct.GroupedProductItemWithRules,
        parentItem: Order.Item,
        quantityToAdd: Int
    ) {
        val newQuantity = parentItem.quantity + quantityToAdd
        val discountAmount = parentItem.subtotal - parentItem.total
        val newSubtotal = parentItem.pricePreDiscount.multiply(newQuantity.toBigDecimal())

        this[product.item.itemId] = parentItem.copy(
            quantity = newQuantity,
            subtotal = newSubtotal,
            total = newSubtotal - discountAmount,
            configuration = product.getConfiguration()
        )

        product.children.forEach { child ->
            this[child.item.itemId] = this[child.item.itemId]?.copy(quantity = 0f) ?: return@forEach
        }
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
