package com.woocommerce.android.ui.products.variations.selector

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.VariationSorting
import javax.inject.Inject

class VariationListHandler @Inject constructor(private val repository: VariationSelectorRepository) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = false
    private var pageSize = DEFAULT_PAGE_SIZE
    private var sorting = VariationSorting.MENU_ORDER
    private var includedVariationIds = emptyList<Long>()

    fun getVariationsFlow(productId: Long) = repository.observeVariations(productId)

    suspend fun resetState() {
        mutex.withLock {
            offset = 0
            canLoadMore = false
        }
    }

    fun canLoadMore(numOfVariations: Int): Boolean {
        return canLoadMore || (offset + pageSize < numOfVariations)
    }

    suspend fun fetchVariations(
        productId: Long,
        forceRefresh: Boolean = false,
        filterOptions: Map<WCProductStore.VariationFilterOption, String>? = null,
        orderCurrency: String? = null,
        sorting: VariationSorting = VariationSorting.MENU_ORDER,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        includedVariationIds: List<Long> = emptyList(),
    ): Result<Unit> = mutex.withLock {
        // Reset the offset
        offset = 0
        this.sorting = sorting
        this.pageSize = pageSize
        this.includedVariationIds = includedVariationIds

        if (forceRefresh) {
            loadVariations(productId, filterOptions, orderCurrency)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun loadMore(
        productId: Long,
        filterOptions: Map<WCProductStore.VariationFilterOption, String>? = null,
        orderCurrency: String? = null,
    ): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        loadVariations(productId, filterOptions, orderCurrency)
    }

    private suspend fun loadVariations(
        productId: Long,
        filterOptions: Map<WCProductStore.VariationFilterOption, String>? = null,
        orderCurrency: String? = null,
    ): Result<Unit> {
        return repository.fetchVariations(
            productId,
            offset,
            pageSize,
            filterOptions,
            orderCurrency = orderCurrency,
            sorting = sorting,
            includedVariationIds = includedVariationIds
        ).onSuccess {
            canLoadMore = it
            offset += pageSize
        }.map { }
    }
}
