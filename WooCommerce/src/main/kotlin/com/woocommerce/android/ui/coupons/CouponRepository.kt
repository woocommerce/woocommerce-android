package com.woocommerce.android.ui.coupons

import com.woocommerce.android.WooException
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private var page = 1
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow<String?>(null)
    private val searchResults = MutableStateFlow(emptyList<CouponDataModel>())

    val couponsFlow = searchQuery.flatMapLatest {
        if (it.isNullOrEmpty()) {
            searchResults
        } else {
            store.observeCoupons(site.get())
        }
    }.map {
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

    suspend fun fetchCoupons(searchQuery: String?, forceRefresh: Boolean = false): Result<Unit> {
        this.searchQuery.value = searchQuery
        page = 1
        canLoadMore = true
        return if (searchQuery.isNullOrEmpty()) {
            if (forceRefresh) {
                loadCoupons()
            } else {
                Result.success(Unit)
            }
        } else {
            searchCoupons()
        }
    }

    private suspend fun loadCoupons(): Result<Unit> {
        return store.fetchCoupons(site.get(), page, PAGE_SIZE).let { result ->
            if (result.isError) {
                Result.failure(WooException(result.error))
            } else {
                canLoadMore = result.model!!
                page++
                Result.success(Unit)
            }
        }
    }

    private suspend fun searchCoupons(): Result<Unit> {
        return store.searchCoupons(
            site.get(),
            searchString = searchQuery.value.orEmpty(),
            page = page,
            pageSize = PAGE_SIZE
        )
            .let { result ->
                if (result.isError) {
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
