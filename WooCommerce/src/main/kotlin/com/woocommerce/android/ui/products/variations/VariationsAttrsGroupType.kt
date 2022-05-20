package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.model.ProductVariation

sealed class VariationsAttrsGroupType {
    object None : VariationsAttrsGroupType()
    object Mixed : VariationsAttrsGroupType()
    data class Value(val value: String) : VariationsAttrsGroupType()
}

val Array<ProductVariation>.regularPriceGroupType: VariationsAttrsGroupType
    get() {
        val prices = map { it.regularPrice }
        return when {
            prices.isEmpty() -> VariationsAttrsGroupType.None
            prices.distinct().size == 1 -> {
                VariationsAttrsGroupType.Value(map { it.priceWithCurrency }.first() ?: "")
            }
            else -> VariationsAttrsGroupType.Mixed
        }
    }
