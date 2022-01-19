package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import java.math.BigDecimal

fun Product.createItem(): Order.Item = Order.Item(
    itemId = 0L,
    productId = remoteId,
    variationId = 0L,
    quantity = 1f,
    name = name,
    price = price ?: BigDecimal.ZERO,
    subtotal = price ?: BigDecimal.ZERO,
    totalTax = BigDecimal.ZERO,
    total = price ?: BigDecimal.ZERO,
    sku = sku,
    attributesList = emptyList(),
)

fun ProductVariation.createItem(parentProduct: Product): Order.Item = Order.Item(
    itemId = 0L,
    productId = remoteProductId,
    variationId = remoteVariationId,
    quantity = 1f,
    name = parentProduct.name,
    price = price ?: BigDecimal.ZERO,
    subtotal = price ?: BigDecimal.ZERO,
    totalTax = BigDecimal.ZERO,
    total = price ?: BigDecimal.ZERO,
    sku = sku,
    attributesList = attributes
        .filterNot { it.name.isNullOrEmpty() || it.option.isNullOrEmpty() }
        .map { Order.Item.Attribute(it.name!!, it.option!!) }
)
