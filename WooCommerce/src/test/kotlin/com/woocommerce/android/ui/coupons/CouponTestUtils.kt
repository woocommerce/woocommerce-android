package com.woocommerce.android.ui.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import java.math.BigDecimal
import java.util.Date

object CouponTestUtils {
    fun generateTestCoupon(couponId: Long): Coupon {
        return Coupon(
            id = couponId,
            amount = BigDecimal.TEN,
            minimumAmount = BigDecimal.TEN,
            maximumAmount = BigDecimal("100"),
            dateCreatedGmt = Date(),
            type = Coupon.Type.FixedCart,
            categories = emptyList(),
            products = emptyList(),
            excludedProducts = emptyList(),
            excludedCategories = emptyList()
        )
    }

    fun generateTestCouponPerformance(couponId: Long): CouponPerformanceReport {
        return CouponPerformanceReport(
            couponId = couponId,
            amount = BigDecimal.TEN,
            ordersCount = 1
        )
    }
}
