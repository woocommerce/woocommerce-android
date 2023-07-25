package com.woocommerce.android.ui.orders.creation.coupon.edit

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponValidator @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: CouponStore,
) {
    suspend fun isCouponValid(code: String): CouponValidationResult {
        val couponSearchResult: WooResult<CouponStore.CouponSearchResult> =
            store.searchCoupons(selectedSite.get(), code)
        if (couponSearchResult.isError) {
            return CouponValidationResult.NETWORK_ERROR
        }
        val couponList: List<CouponWithEmails>? = couponSearchResult.model?.coupons
        val validCoupon = couponList?.find { it.coupon.code?.equals(code, ignoreCase = true) == true }

        return when {
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
