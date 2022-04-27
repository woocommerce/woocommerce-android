package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class EditCouponRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val couponStore: CouponStore
) {
    fun observeCoupon(couponId: Long): Flow<Coupon> = couponStore.observeCoupon(selectedSite.get(), couponId)
        .filterNotNull()
        .map { it.toAppModel() }
}
