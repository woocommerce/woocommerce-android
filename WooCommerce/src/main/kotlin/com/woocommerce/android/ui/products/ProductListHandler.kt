package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ProductListHandler @Inject constructor(private val repository: ProductRepository) {
    companion object {
        private const val PAGE_SIZE = 10
    }

    private val mutex = Mutex()
    private var offset = 0
    private var canLoadMore = true

    private val searchQuery = MutableStateFlow<String?>(null)
    private val searchResults = MutableStateFlow(emptyList<Product>())

    val productsFlow = searchQuery.flatMapLatest {
        if (it == null) {
            repository.observeProducts()
        } else {
            searchResults
        }
    }

    suspend fun fetchProducts(
        searchQuery: String? = null,
        forceRefresh: Boolean = false
    ): Result<Unit> = mutex.withLock {
        // Reset the offset
        offset = 0
        canLoadMore = true

        this.searchQuery.value = searchQuery
        return if (searchQuery == null) {
            if (forceRefresh) {
                loadProducts()
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
                searchProducts()
            }
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        return if (searchQuery.value == null) {
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
            searchQuery = searchQuery.value!!,
        ).onSuccess { result ->
            canLoadMore = result.canLoadMore
            offset += PAGE_SIZE
            searchResults.update { it + result.products }
        }.map { }
    }
}
