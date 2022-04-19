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
    companion object {
        private const val PAGE_SIZE = 10
    }
    private var page = 1
    private var canLoadMore = true

    val couponsFlow = store.observeCoupons(site.get()).map {
        it.map { couponDataModel -> couponDataModel.toAppModel() }
    }

    suspend fun loadCoupons(loadMore: Boolean = false) {
        if (!loadMore) {
            page = 1
        } else if (!canLoadMore) {
            return
        }
        val result = store.fetchCoupons(site.get(), page++, PAGE_SIZE)
        canLoadMore = result.model ?: false
    }
}
