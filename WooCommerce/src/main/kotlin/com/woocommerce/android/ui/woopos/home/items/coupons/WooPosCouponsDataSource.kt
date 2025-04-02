


package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.ui.woopos.home.items.common.FetchOptions
import com.woocommerce.android.ui.woopos.home.items.common.WooPosBaseDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCouponsDataSource @Inject constructor(
    private val handler: CouponListHandler,
) : WooPosBaseDataSource<Coupon>() {
    private var couponCache: List<Coupon> = emptyList()
    private val cacheMutex = Mutex()

    val hasMorePages: Boolean
        get() = handler.canLoadMore

    override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<Coupon> {
        return couponCache
    }

    override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<Coupon>> {
        val result = handler.fetchCoupons(
            forceRefresh = true
        )
        return if (result.isSuccess) {
            Result.success(handler.couponsFlow.first())
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error while fetching products"))
        }
    }

    override suspend fun updateCache(fetchOptions: FetchOptions, data: List<Coupon>) {
        updateCouponCache(data)
    }

    private suspend fun updateCouponCache(newList: List<Coupon>) {
        cacheMutex.withLock {
            couponCache = newList
        }
    }

    suspend fun loadMore(): Result<List<Coupon>> = fetchMore(
        fetchMore = {
            handler.loadMore()
                .map { handler.couponsFlow.first() }
        }
    )
}

