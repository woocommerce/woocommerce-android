package com.woocommerce.android.ui.woopos.util.format

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.util.CouponUtils
import javax.inject.Inject

class WooPosFormatCouponSummary @Inject constructor(
    private val couponUtils: CouponUtils
) {
    fun formatCouponSummary(coupon: Coupon, currencyCode: String): String {
        return couponUtils.generateSummary(coupon, currencyCode)
    }
}
