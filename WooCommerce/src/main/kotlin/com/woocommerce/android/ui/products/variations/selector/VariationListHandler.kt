package com.woocommerce.android.ui.products.variations.selector

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class VariationListHandler @Inject constructor(private val repository: VariationSelectorRepository) {
    companion object {
        private const val PAGE_SIZE = 10
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = true

    fun getVariationsFlow(productId: Long) = repository.observeVariations(productId)

    suspend fun fetchVariations(productId: Long, forceRefresh: Boolean = false): Result<Unit> = mutex.withLock {
        // Reset the offset
        offset = 0
        canLoadMore = true

        if (forceRefresh) {
            loadVariations(productId)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun loadMore(productId: Long): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        loadVariations(productId)
    }

    private suspend fun loadVariations(productId: Long): Result<Unit> {
        return repository.fetchVariations(productId, offset, PAGE_SIZE).onSuccess {
            canLoadMore = it
            offset += PAGE_SIZE
        }.map { }
    }
}
