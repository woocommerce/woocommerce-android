package com.woocommerce.android.ui.products.variations

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class VariationListPaginationHandler @Inject constructor(private val repository: VariationRepository) {
    private val mutex = Mutex()
    private var nextOffset = 0

    var canLoadMore = false
        private set

    suspend fun fetchVariations(remoteProductId: Long): Result<Unit> = mutex.withLock {
        loadPage(remoteProductId, offset = 0)
    }

    suspend fun loadMore(remoteProductId: Long): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        loadPage(remoteProductId, offset = nextOffset)
    }

    private suspend fun loadPage(remoteProductId: Long, offset: Int): Result<Unit> {
        return repository.fetchProductVariationsPage(
            remoteProductId = remoteProductId,
            offset = offset,
            pageSize = VariationListPaging.PAGE_SIZE,
            sorting = VariationListPaging.SORTING
        ).onSuccess {
            canLoadMore = it
            nextOffset = offset + VariationListPaging.PAGE_SIZE
        }.map { }
    }
}
