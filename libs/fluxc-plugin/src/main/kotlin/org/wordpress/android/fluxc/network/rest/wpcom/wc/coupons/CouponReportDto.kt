package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.coupons.CouponReport
import java.math.BigDecimal

data class CouponReportDto(
    @SerializedName("coupon_id") val couponId: Long,
    @SerializedName("amount") val amount: String?,
    @SerializedName("orders_count") val ordersCount: Int?
)

fun CouponReportDto.toDataModel() = CouponReport(
    couponId = couponId,
    amount = amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    ordersCount = ordersCount ?: 0
)
