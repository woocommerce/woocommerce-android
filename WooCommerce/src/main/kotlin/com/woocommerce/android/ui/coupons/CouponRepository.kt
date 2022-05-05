package com.woocommerce.android.ui.coupons

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite
) {
    suspend fun fetchCoupons(
        page: Int,
        pageSize: Int
    ): Result<Boolean> {
        return store.fetchCoupons(site.get(), page, pageSize)
            .let { result ->
                if (result.isError) {
                    AnalyticsTracker.track(
                        AnalyticsEvent.COUPONS_LOAD_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to result.error::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.name,
                            AnalyticsTracker.KEY_ERROR_DESC to result.error.message
                        )
                    )

                    WooLog.w(
                        WooLog.T.COUPONS,
                        "Fetching coupons failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    AnalyticsTracker.track(
                        AnalyticsEvent.COUPONS_LOADED,
                        mapOf(Pair(AnalyticsTracker.KEY_IS_LOADING_MORE, page > 1))
                    )
                    Result.success(result.model!!)
                }
            }
    }

    suspend fun searchCoupons(
        searchString: String,
        page: Int,
        pageSize: Int
    ): Result<SearchResult> {
        return store.searchCoupons(
            site.get(),
            searchString = searchString,
            page = page,
            pageSize = pageSize
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.COUPONS,
                    "Searching coupons failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                Result.success(
                    SearchResult(
                        coupons = searchResult.coupons.map { couponDataModel -> couponDataModel.toAppModel() },
                        canLoadMore = searchResult.canLoadMore
                    )
                )
            }
        }
    }

    fun observeCoupons(): Flow<List<Coupon>> = store.observeCoupons(site.get()).map {
        it.map { couponDataModel -> couponDataModel.toAppModel() }
    }

    data class SearchResult(
        val coupons: List<Coupon>,
        val canLoadMore: Boolean
    )
}
