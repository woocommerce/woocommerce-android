package com.woocommerce.android.ui.coupons

data class CouponListItem(
    val id: Long,
    val code: String? = null,
    val summary: String,
    val isActive: Boolean
)
