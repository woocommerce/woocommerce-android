package com.woocommerce.android.ui.woopos.util.format

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.util.CouponUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class WooPosCouponsFormatter @Inject constructor(
    private val couponUtils: CouponUtils,
) {
    private val expiryDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    fun formatSummary(coupon: Coupon, currencyCode: String): String {
        return couponUtils.generateSummary(coupon, currencyCode)
    }

    fun formatExpiredText(couponExpireDate: LocalDate): String {
        return expiryDateFormatter.format(couponExpireDate)
    }
}
