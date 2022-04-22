package com.woocommerce.android.ui.coupons.details

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponDetailsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: CouponStore
) {
    fun observeCoupon(couponId: Long): Flow<Coupon> = store.observeCoupon(selectedSite.get(), couponId)
        .filterNotNull()
        .map { it.toAppModel() }

    suspend fun fetchCoupon(couponId: Long): Result<Unit> {
        val result = store.fetchCoupon(selectedSite.get(), couponId)
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun fetchCouponPerformance(couponId: Long): Result<CouponPerformanceReport> {
        val result = store.fetchCouponReport(selectedSite.get(), couponId)

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model!!.toAppModel())
        }
    }

    suspend fun deleteCoupon(couponId: Long): Boolean {
        val result = store.deleteCoupon(
            site = selectedSite.get(),
            couponId = couponId
        )

        return if (result.isError) {
            WooLog.e(
                tag = WooLog.T.COUPONS,
                message = "Coupon deletion failed: ${result.error.message}"
            )
            false
        } else {
            true
        }
    }
}
