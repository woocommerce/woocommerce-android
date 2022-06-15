package com.woocommerce.android.ui.products.categories.selector

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ProductCategoryListHandler @Inject constructor(
    private val repository: ProductCategorySelectorRepository
) {
    companion object {
        @VisibleForTesting
        const val PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow("")
    private val searchResults = MutableStateFlow(emptyList<ProductCategoryTreeItem>())

    val categories: Flow<List<ProductCategoryTreeItem>> = searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            repository.observeCategories()
                .map { it.convertToTree() }
        } else {
            searchResults
        }
    }

    suspend fun fetchCategories(
        forceRefresh: Boolean = false,
        searchQuery: String = ""
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        offset = 0
        canLoadMore = true

        this.searchQuery.value = searchQuery
        return@withLock if (searchQuery.isEmpty()) {
            if (forceRefresh) {
                loadCategories()
            } else {
                Result.success(Unit)
            }
        } else {
            searchResults.value = emptyList()
            searchCategories()
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        return if (searchQuery.value.isEmpty()) {
            loadCategories()
        } else {
            searchCategories()
        }
    }

    private suspend fun loadCategories(): Result<Unit> {
        return repository.fetchCategories(offset, PAGE_SIZE).onSuccess {
            canLoadMore = it
            offset += PAGE_SIZE
        }.map { }
    }

    private suspend fun searchCategories(): Result<Unit> {
        return repository.searchCategories(
            searchQuery = searchQuery.value,
            offset = offset,
            pageSize = PAGE_SIZE
        ).onSuccess { result ->
            // Return results as flat tree
            val mappedResults = result.productCategories.convertToFlatTree()
            searchResults.update { it + mappedResults }
            canLoadMore = result.canLoadMore
            offset += PAGE_SIZE
        }.map { }
    }
}
