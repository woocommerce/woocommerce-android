package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.domain.Addon
import javax.inject.Inject

sealed class OrderProduct {
    data class ProductItem(val product: Order.Item, val addons: List<Addon>) : OrderProduct()

    data class GroupedProductItem(
        val product: Order.Item,
        val children: List<ProductItem>,
        var isExpanded: Boolean = false
    ) : OrderProduct()
}

class OrderProductMapper @Inject constructor() {
    fun toOrderProducts(
        currentProducts: List<OrderProduct>,
        newProductsWithAddons: List<Pair<Order.Item, List<Addon>>>
    ): List<OrderProduct> {
        if (newProductsWithAddons.isEmpty()) return emptyList()

        val newProducts = newProductsWithAddons.map { it.first }

        val isExpanded = currentProducts
            .filterIsInstance<OrderProduct.GroupedProductItem>()
            .associate { Pair(it.product.itemId, it.isExpanded) }

        val itemsMap = newProducts.associateBy { item -> item.itemId }
        val childrenMap = mutableMapOf<Long, MutableList<OrderProduct.ProductItem>>()

        val result = newProductsWithAddons.mapNotNull { (item, addons) ->
            if (item.parent == null) {
                item to addons
            } else {
                val children = childrenMap.getOrPut(
                    item.parent
                ) { mutableListOf() }

                children.add(OrderProduct.ProductItem(item, addons))
                null
            }
        }.filter { (item, addons) ->
            (item.itemId in childrenMap.keys).not()
        }.map<Pair<Order.Item, List<Addon>>, OrderProduct> { (item, addons) -> OrderProduct.ProductItem(item, addons) }
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
