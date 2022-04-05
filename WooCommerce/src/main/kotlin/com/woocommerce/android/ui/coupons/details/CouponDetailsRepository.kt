package com.woocommerce.android.ui.coupons.details

import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.store.CouponStore
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
class CouponDetailsRepository @Inject constructor(private val store: CouponStore) {
    // TODO This should return a [com.woocommerce.android.model.Coupon] instead of [CouponUi]
    @Suppress("MagicNumber")
    fun loadCoupon(couponId: Long): Flow<CouponUi> {
        return flowOf(
            CouponUi(
                id = 1,
                code = "ABCDE",
                amount = BigDecimal(25),
                formattedDiscount = "25%",
                affectedArticles = "Everything excl. 5 products",
                formattedSpendingInfo = "Minimum spend of $20 \n\nMaximum spend of $200 \n",
                isActive = true
            )
        )
    }
}
