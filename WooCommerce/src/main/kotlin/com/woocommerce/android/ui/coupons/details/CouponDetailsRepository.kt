package com.woocommerce.android.ui.coupons.details

import com.woocommerce.android.WooException
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.store.CouponStore
import java.math.BigDecimal
import javax.inject.Inject

class CouponDetailsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: CouponStore
) {
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

    suspend fun fetchCouponPerformance(couponId: Long): Result<CouponPerformanceReport> {
        val result = store.fetchCouponReport(selectedSite.get(), couponId)

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model!!.toAppModel())
        }
    }
}
