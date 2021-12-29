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
    price = regularPrice ?: BigDecimal.ZERO, // TODO add a price property to the Product
    subtotal = regularPrice ?: BigDecimal.ZERO,
    totalTax = BigDecimal.ZERO,
    total = regularPrice ?: BigDecimal.ZERO,
    sku = sku,
    attributesList = emptyList(),
)

fun ProductVariation.createItem(parentProduct: Product): Order.Item = Order.Item(
    itemId = 0L,
    productId = remoteProductId,
    variationId = remoteVariationId,
    quantity = 1f,
    name = parentProduct.name,
    price = regularPrice ?: BigDecimal.ZERO, // TODO add a price property to the Product
    subtotal = regularPrice ?: BigDecimal.ZERO,
    totalTax = BigDecimal.ZERO,
    total = regularPrice ?: BigDecimal.ZERO,
    sku = sku,
    attributesList = attributes
        .filterNot { it.name.isNullOrEmpty() || it.option.isNullOrEmpty() }
        .map { Order.Item.Attribute(it.name!!, it.option!!) }
)
