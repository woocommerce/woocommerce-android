package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttributeTerm

val defaultAttributeList = generateAttributeList(from = 1, to = 10)

val defaultLoadMoreList = generateAttributeList(from = 11, to = 16)

fun generateAttributeList(from: Int, to: Int): List<ProductAttributeTerm> {
    return (from..to).map { index ->
        ProductAttributeTerm(
            id = index,
            remoteId = index,
            name = "Term $index",
            slug = "term-$index",
            description = "Term $index description"
        )
    }
}
