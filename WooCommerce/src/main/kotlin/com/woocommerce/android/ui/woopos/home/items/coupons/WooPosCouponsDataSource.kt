package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.IDLE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.LOADING_MORE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCouponsDataSource @Inject constructor(private val handler: CouponListHandler) {
    private val _fetchingState: MutableStateFlow<FetchingCouponsState> = MutableStateFlow(IDLE)
    val fetchingState: Flow<FetchingCouponsState> = _fetchingState

    val couponsFlow: Flow<List<Coupon>> = handler.couponsFlow

    suspend fun clearCacheAndFetchFirstPage(): Result<Unit> {
        _fetchingState.emit(FETCHING_FIRST_PAGE)
        return handler.fetchCoupons(searchQuery = null, forceRefresh = true).also {
            _fetchingState.emit(IDLE)
        }
    }

    suspend fun loadMore(): Result<Unit> {
        _fetchingState.emit(LOADING_MORE)
        return handler.loadMore().also {
            _fetchingState.emit(IDLE)
        }
    }

    enum class FetchingCouponsState {
        IDLE, LOADING_MORE, FETCHING_FIRST_PAGE
    }
}
