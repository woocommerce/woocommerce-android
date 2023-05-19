package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponValidator @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: CouponStore,
) {
    suspend fun isCouponValid(code: String): Boolean {
        val result = store.searchCoupons(selectedSite.get(), code).model ?: return false
        return result.coupons.find { code == it.coupon.code } != null
    }
}
