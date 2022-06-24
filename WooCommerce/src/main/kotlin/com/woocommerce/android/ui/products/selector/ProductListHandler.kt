package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ProductListHandler @Inject constructor(private val repository: ProductSelectorRepository) {
    companion object {
        private const val PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow("")
    private val searchResults = MutableStateFlow(emptyList<Product>())

    val productsFlow = searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            repository.observeProducts()
        } else {
            searchResults
        }
    }

    suspend fun fetchProducts(
        searchQuery: String = "",
        forceRefresh: Boolean = false
    ): Result<Unit> = mutex.withLock {
        // Reset the offset
        offset = 0
        canLoadMore = true

        this.searchQuery.value = searchQuery
        return if (searchQuery.isEmpty()) {
            if (forceRefresh) {
                loadProducts()
            } else {
                Result.success(Unit)
            }
        } else {
            searchResults.value = emptyList()
            searchProducts()
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        return if (searchQuery.value.isEmpty()) {
            loadProducts()
        } else {
            searchProducts()
        }
    }

    private suspend fun loadProducts(): Result<Unit> {
        return repository.fetchProducts(offset, PAGE_SIZE).onSuccess {
            canLoadMore = it
            offset += PAGE_SIZE
        }.map { }
    }

    private suspend fun searchProducts(): Result<Unit> {
        return repository.searchProducts(
            offset = offset,
            pageSize = PAGE_SIZE,
            searchQuery = searchQuery.value,
        ).onSuccess { result ->
            canLoadMore = result.canLoadMore
            offset += PAGE_SIZE
            searchResults.update { it + result.products }
        }.map { }
    }
}
