package com.woocommerce.android.ui.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.util.CouponUtils
import java.util.Date

fun Coupon.toUiModel(couponUtils: CouponUtils, currencyCode: String?): CouponListItem {
    return CouponListItem(
        id = id,
        code = code,
        summary = couponUtils.generateSummary(this, currencyCode),
        isActive = dateExpires?.after(Date()) ?: true
    )
}
