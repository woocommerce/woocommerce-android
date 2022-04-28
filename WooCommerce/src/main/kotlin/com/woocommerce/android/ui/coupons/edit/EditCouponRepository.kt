package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class EditCouponRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val couponStore: CouponStore
) {
    // TODO replace this with the added [getCoupon] function when the last FluxC PR is merged
    suspend fun getCoupon(couponId: Long): Coupon = couponStore.observeCoupon(selectedSite.get(), couponId)
        .filterNotNull()
        .map { it.toAppModel() }
        .first()
}
