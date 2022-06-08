package com.woocommerce.android.ui.products.categories.selector

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ProductCategoryListHandler @Inject constructor(
    private val repository: ProductCategorySelectorRepository
) {
    companion object {
        private const val PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = true

    val categories: Flow<List<ProductCategoryTreeItem>> = repository.observeCategories()
        .map { it.convertToTree() }

    suspend fun fetchCategories(
        forceRefresh: Boolean = false
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        offset = 0
        canLoadMore = true

        return if (forceRefresh) {
            loadCategories()
        } else {
            Result.success(Unit)
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        return loadCategories()
    }

    private suspend fun loadCategories(): Result<Unit> {
        return repository.fetchCategories(offset, PAGE_SIZE).onSuccess {
            canLoadMore = it
            offset += PAGE_SIZE
        }.map { }
    }
}
