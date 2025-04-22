package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCouponsDataSource @Inject constructor(private val handler: CouponListHandler) {
    private val _isFetching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFetching: Flow<Boolean> = _isFetching

    val couponsFlow: Flow<List<Coupon>> = handler.couponsFlow

    suspend fun clearCacheAndFetchFirstPage(): Result<Unit> {
        _isFetching.emit(true)
        return handler.fetchCoupons(searchQuery = null, forceRefresh = true).also {
            _isFetching.emit(false)
        }
    }

    suspend fun loadMore(): Result<Unit> {
        _isFetching.emit(true)
        return handler.loadMore().also {
            _isFetching.emit(false)
        }
    }
}
