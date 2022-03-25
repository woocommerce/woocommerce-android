package com.woocommerce.android.ui.coupons

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite
) {
    val couponsFlow by lazy {
        store.observeCoupons(site.get())
    }

    suspend fun loadCoupons() {
        store.fetchCoupons(site.get())
    }
}
