package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.content.Context
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class CouponValidator @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: CouponStore,
    private val context: Context,
) {
    suspend fun isCouponValid(code: String): CouponValidationResult {
        val networkAvailable = NetworkUtils.isNetworkAvailable(context)
        val coupons = store.searchCoupons(selectedSite.get(), code).model?.coupons
        val validCoupon = coupons?.find { it.coupon.code?.equals(code, ignoreCase = true) == true }

        return when {
            !networkAvailable -> CouponValidationResult.NETWORK_ERROR
            validCoupon != null -> CouponValidationResult.VALID
            else -> CouponValidationResult.INVALID
        }
    }

    enum class CouponValidationResult {
        VALID,
        INVALID,
        NETWORK_ERROR,
    }
}
