package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import com.woocommerce.android.model.Order
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class OrderProduct : Parcelable {
    @Parcelize
    data class ProductItem(val product: Order.Item) : OrderProduct()

    @Parcelize
    data class GroupedProductItem(val product: Order.Item, val children: List<ProductItem>) : OrderProduct()
}

fun List<Order.Item>.toOrderProducts(): List<OrderProduct> {
    if (isEmpty()) return emptyList()

    val itemsMap = associateBy { item -> item.itemId }
    val childrenMap = mutableMapOf<Long, MutableList<OrderProduct.ProductItem>>()

    val result = mapNotNull { item ->
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

        val groupedProduct = OrderProduct.GroupedProductItem(parent, children)
        result.add(groupedProduct)
    }
    return result
}
