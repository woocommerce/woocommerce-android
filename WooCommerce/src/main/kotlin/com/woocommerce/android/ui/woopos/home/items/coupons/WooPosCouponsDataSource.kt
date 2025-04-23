package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.coupons.CouponListHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("unused")
class WooPosCouponsDataSource @Inject constructor(private val handler: CouponListHandler) {
    suspend fun clearCacheAndFetchFirstPage(): Result<Unit> {
        return handler.fetchCoupons(searchQuery = null, forceRefresh = true)
    }

    suspend fun loadMore(): Result<Unit> {
        return handler.loadMore()
    }
}
