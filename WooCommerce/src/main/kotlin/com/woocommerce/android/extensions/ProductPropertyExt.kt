package com.woocommerce.android.extensions

import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Property
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductPropertyCard

fun MutableList<ProductPropertyCard>.addIfNotEmpty(card: ProductPropertyCard) {
    if (card.properties.isNotEmpty()) {
        add(card)
    }
}

fun MutableList<ProductProperty>.addPropertyIfNotEmpty(item: ProductProperty) {
    when (item) {
        is Property -> if (item.value.isNotBlank()) add(item)
        is ComplexProperty -> if (item.value.isNotBlank()) add(item)
        is PropertyGroup -> if (item.properties.filter { it.value.isNotBlank() }.isNotEmpty()) add(item)
    }
}
