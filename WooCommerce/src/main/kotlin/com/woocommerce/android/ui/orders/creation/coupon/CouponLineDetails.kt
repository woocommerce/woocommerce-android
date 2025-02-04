package com.woocommerce.android.ui.orders.creation.coupon

data class CouponLineDetails(
    val code: String
)

data class CouponSection(
    val couponLines: List<CouponLineDetails>,
    val isEnabled: Boolean,
)
