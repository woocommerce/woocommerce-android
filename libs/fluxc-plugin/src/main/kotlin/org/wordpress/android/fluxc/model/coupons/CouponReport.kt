package org.wordpress.android.fluxc.model.coupons

import java.math.BigDecimal

data class CouponReport(
    val couponId: Long,
    val amount: BigDecimal,
    val ordersCount: Int
)
