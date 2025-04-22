package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCouponsDataSource @Inject constructor(private val handler: CouponListHandler) {
    val couponsFlow: Flow<List<Coupon>> = handler.couponsFlow

    suspend fun clearCacheAndFetchFirstPage() =
        handler.fetchCoupons(searchQuery = null, forceRefresh = true)

    suspend fun loadMore() = handler.loadMore()
}
