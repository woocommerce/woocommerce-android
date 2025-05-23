package com.woocommerce.android.ui.woopos.home.items.coupons.search

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WooPosCouponsSearchDataSource @Inject constructor(private val handler: CouponListHandler) {
    val couponsFlow: Flow<List<Coupon>> = handler.couponsFlow

    suspend fun searchCoupons(query: String): Result<Boolean> {
        return handler.fetchCoupons(searchQuery = query, forceRefresh = true)
    }

    suspend fun loadMore(): Result<Boolean> {
        return handler.loadMore()
    }
}
