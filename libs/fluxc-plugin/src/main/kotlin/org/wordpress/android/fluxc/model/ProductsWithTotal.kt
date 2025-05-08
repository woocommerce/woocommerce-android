package org.wordpress.android.fluxc.model

data class ProductsWithTotal(
    val products: List<ProductWithMetaData>,
    val totalCount: Int?
)
