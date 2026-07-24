package com.woocommerce.android.ui.woopos.home

import java.math.BigDecimal

data class WooPosOrderCreatedData(
    val updatedProducts: List<ProductInfo>,
    val updatedCoupons: List<CouponInfo>,
    val wholeCartCouponDiscountApplied: Boolean = false,
) {
    sealed class ProductInfo(
        open val id: Long,
        open val name: String,
        open val finalPrice: BigDecimal,
        open val basePrice: BigDecimal,
        open val quantity: Float,
    ) {
        data class Simple(
            override val id: Long,
            override val name: String,
            override val finalPrice: BigDecimal,
            override val basePrice: BigDecimal,
            override val quantity: Float,
        ) : ProductInfo(id, name, finalPrice, basePrice, quantity)

        data class Variation(
            override val id: Long,
            override val name: String,
            override val finalPrice: BigDecimal,
            override val basePrice: BigDecimal,
            override val quantity: Float,
            val variationId: Long,
        ) : ProductInfo(id, name, finalPrice, basePrice, quantity)
    }

    data class CouponInfo(
        val id: Long,
        val code: String,
        val discountAmount: BigDecimal,
    )
}
