package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import javax.inject.Inject

sealed class OrderProduct(open val product: Order.Item) {
    data class ProductItem(override val product: Order.Item) : OrderProduct(product)

    data class GroupedProductItem(
        override val product: Order.Item,
        val children: List<ProductItem>,
        var isExpanded: Boolean = false
    ) : OrderProduct(product)
}

class OrderProductMapper @Inject constructor() {
    fun toOrderProducts(currentProducts: List<OrderProduct>, newProducts: List<Order.Item>): List<OrderProduct> {
        if (newProducts.isEmpty()) return emptyList()

        val isExpanded = currentProducts
            .filterIsInstance<OrderProduct.GroupedProductItem>()
            .associate { Pair(it.product.itemId, it.isExpanded) }

        val itemsMap = newProducts.associateBy { item -> item.itemId }
        val childrenMap = mutableMapOf<Long, MutableList<OrderProduct.ProductItem>>()

        val result = newProducts.mapNotNull { item ->
            if (item.parent == null) {
                item
            } else {
                val children = childrenMap.getOrPut(
                    item.parent
                ) { mutableListOf() }

                children.add(OrderProduct.ProductItem(item))
                null
            }
        }.filter { item ->
            (item.itemId in childrenMap.keys).not()
        }.map<Order.Item, OrderProduct> { OrderProduct.ProductItem(it) }
            .toMutableList()

        for (parentId in childrenMap.keys) {
            val parent = itemsMap[parentId] ?: continue
            val children = childrenMap[parentId] ?: emptyList()

            val groupedProduct =
                OrderProduct.GroupedProductItem(
                    parent,
                    children,
                    isExpanded.getOrDefault(parent.itemId, false)
                )
            result.add(groupedProduct)
        }
        return result
    }
}
