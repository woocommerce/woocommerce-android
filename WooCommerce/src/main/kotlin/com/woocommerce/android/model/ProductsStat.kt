package com.woocommerce.android.model

data class ProductsStat(
    val itemsSold: Int,
    val itemsSoldDelta: DeltaPercentage,
    val products: List<ProductItem>
) {
    companion object {
        val EMPTY = ProductsStat(
            itemsSold = 0,
            itemsSoldDelta = DeltaPercentage.NotExist,
            products = emptyList()
        )
    }
}

data class ProductItem(
    val name: String,
    val netSales: Double,
    val image: String?,
    val quantity: Int,
    val currencyCode: String?
)
