package com.woocommerce.android.extensions

import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard

fun MutableList<ProductPropertyCard>.addIfNotEmpty(card: ProductPropertyCard?) {
    if (card != null && card.properties.isNotEmpty()) {
        add(card)
    }
}

fun List<ProductProperty?>.filterNotEmpty(): List<ProductProperty> {
    return this.filterNotNull().filter { it.isNotEmpty() }
}
