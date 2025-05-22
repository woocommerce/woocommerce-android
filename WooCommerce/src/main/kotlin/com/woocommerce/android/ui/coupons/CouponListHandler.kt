package com.woocommerce.android.ui.coupons

import com.woocommerce.android.model.Coupon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CouponListHandler @Inject constructor(private val repository: CouponRepository) {
    companion object {
        private const val PAGE_SIZE = 10
    }

    private val mutex = Mutex()
    private var page = 1
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow<String?>(null)
    private val searchResults = MutableStateFlow(emptyList<Coupon>())

    val couponsFlow = searchQuery.flatMapLatest {
        if (it == null) {
            repository.observeCoupons()
        } else {
            searchResults
        }
    }

    suspend fun fetchCoupons(
        searchQuery: String? = null,
        forceRefresh: Boolean = false,
        isPos: Boolean = false
    ): Result<Boolean> = mutex.withLock {
        // Reset pagination attributes
        page = 1
        canLoadMore = true

        this.searchQuery.value = searchQuery
        return if (searchQuery == null) {
            if (forceRefresh) {
                loadCoupons(isPos)
            } else {
                Result.success(canLoadMore)
            }
        } else {
            searchResults.value = emptyList()
            if (searchQuery.isEmpty()) {
                // If the query is empty, clear search results directly
                canLoadMore = false
                Result.success(canLoadMore)
            } else {
                searchCoupons()
            }
        }
    }

    suspend fun loadMore(isPos: Boolean = false): Result<Boolean> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(canLoadMore)
        return if (searchQuery.value == null) {
            loadCoupons(isPos)
        } else {
            searchCoupons()
        }
    }

    private suspend fun loadCoupons(isPos: Boolean): Result<Boolean> {
        return repository.fetchCoupons(page, PAGE_SIZE, isPosMode = isPos).onSuccess {
            canLoadMore = it
            page++
        }.map { canLoadMore }
    }

    private suspend fun searchCoupons(): Result<Boolean> {
        return repository.searchCoupons(
            searchString = searchQuery.value!!,
            page = page,
            pageSize = PAGE_SIZE
        ).onSuccess { result ->
            canLoadMore = result.canLoadMore
            page++
            searchResults.update { it + result.coupons }
        }.map { canLoadMore }
    }
}
