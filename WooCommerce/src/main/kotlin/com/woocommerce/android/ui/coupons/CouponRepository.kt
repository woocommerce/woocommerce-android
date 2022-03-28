package com.woocommerce.android.ui.coupons

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite
) {
    val couponsFlow = store.observeCoupons(site.get()).map {
        it.map { couponDataModel -> couponDataModel.toAppModel() }
    }

    suspend fun loadCoupons() {
        store.fetchCoupons(site.get())
    }
}
