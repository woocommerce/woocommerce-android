package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.coupons.CouponReport
import java.math.BigDecimal

@Parcelize
data class CouponPerformanceReport(
    val couponId: Long,
    val ordersCount: Int,
    val amount: BigDecimal
) : Parcelable

fun CouponReport.toAppModel() = CouponPerformanceReport(
    couponId = couponId,
    ordersCount = ordersCount,
    amount = amount
)
