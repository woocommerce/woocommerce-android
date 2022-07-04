package com.woocommerce.android.ui.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.model.CouponPerformanceReport
import java.math.BigDecimal
import java.util.Date

object CouponTestUtils {
    fun generateTestCoupon(couponId: Long): Coupon {
        return Coupon(
            id = couponId,
            code = "code1337",
            amount = BigDecimal.TEN,
            dateCreatedGmt = Date(),
            type = Coupon.Type.FixedCart,
            categoryIds = emptyList(),
            productIds = emptyList(),
            restrictions = CouponRestrictions(
                minimumAmount = BigDecimal.TEN,
                maximumAmount = BigDecimal("100"),
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList(),
            )
        )
    }

    fun generateCouponRestrictions() = CouponRestrictions(
        minimumAmount = BigDecimal.TEN,
        maximumAmount = BigDecimal("100"),
        excludedProductIds = emptyList(),
        excludedCategoryIds = emptyList(),
        restrictedEmails = emptyList(),
    )

    fun generateTestCouponPerformance(couponId: Long): CouponPerformanceReport {
        return CouponPerformanceReport(
            couponId = couponId,
            amount = BigDecimal.TEN,
            ordersCount = 1
        )
    }
}
