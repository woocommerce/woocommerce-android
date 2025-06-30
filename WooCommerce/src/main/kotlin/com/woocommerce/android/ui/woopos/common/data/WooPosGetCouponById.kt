package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class WooPosGetCouponById @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite,
) {
    suspend operator fun invoke(couponId: Long): Coupon? = withContext(IO) {
        store.getCoupon(site.get(), couponId)?.toAppModel()
    }
}
