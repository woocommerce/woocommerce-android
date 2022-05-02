package com.woocommerce.android.ui.coupons

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite
) {
    companion object {
        private const val PAGE_SIZE = 10
    }

    private val mutex = Mutex()

    private var page = 1
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow<String?>(null)
    private val searchResults = MutableStateFlow(emptyList<CouponDataModel>())

    val couponsFlow = searchQuery.flatMapLatest {
        if (it == null) {
            store.observeCoupons(site.get())
        } else {
            searchResults
        }
    }.map {
        it.map { couponDataModel -> couponDataModel.toAppModel() }
    }

    suspend fun fetchCoupons(
        searchQuery: String? = null,
        forceRefresh: Boolean = false
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page = 1
        canLoadMore = true

        this.searchQuery.value = searchQuery
        return if (searchQuery == null) {
            if (forceRefresh) {
                loadCoupons()
            } else {
                Result.success(Unit)
            }
        } else {
            searchResults.value = emptyList()
            if (searchQuery.isEmpty()) {
                // If the query is empty, clear search results directly
                canLoadMore = false
                Result.success(Unit)
            } else {
                searchCoupons()
            }
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        return if (searchQuery.value == null) {
            loadCoupons()
        } else {
            searchCoupons()
        }
    }

    private suspend fun loadCoupons(): Result<Unit> {
        return store.fetchCoupons(site.get(), page, PAGE_SIZE).let { result ->
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
                canLoadMore = result.model!!
                page++
                Result.success(Unit)
            }
        }
    }

    private suspend fun searchCoupons(): Result<Unit> {
        return store.searchCoupons(
            site.get(),
            searchString = searchQuery.value!!,
            page = page,
            pageSize = PAGE_SIZE
        )
            .let { result ->
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.COUPONS,
                        "Searching coupons failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    val searchResult = result.model!!
                    searchResults.update { it + searchResult.coupons }
                    canLoadMore = searchResult.canLoadMore
                    page++
                    Result.success(Unit)
                }
            }
    }
}
