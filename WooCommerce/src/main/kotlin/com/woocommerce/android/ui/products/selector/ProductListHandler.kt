package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
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

    private val productFilters = MutableStateFlow(mapOf<ProductFilterOption, String>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsFlow = combine(searchQuery, productFilters) { query, filters ->
        if (query.isEmpty()) {
            repository.observeProducts(filters)
        } else {
            searchResults
        }
    }.flatMapLatest { it }

    suspend fun loadFromCacheAndFetch(
        searchQuery: String = "",
        forceRefresh: Boolean = false,
        filters: Map<ProductFilterOption, String> = emptyMap()
    ): Result<Unit> = mutex.withLock {
        offset = 0
        canLoadMore = true
        searchResults.value = emptyList()

        this.searchQuery.value = searchQuery
        this.productFilters.value = filters

        return if (searchQuery.isNotEmpty()) {
            searchInCache()
            remoteSearch()
        } else {
            if (forceRefresh) {
                fetchProducts()
            } else {
                Result.success(Unit)
            }
        }
    }

    // The implementation of loadMore has limited functionality. Essentially, more items from local cache are loaded
    // only after the remote request to fetch the previous page finishes successfully.
    suspend fun loadMore() = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        if (searchQuery.value.isEmpty()) {
            fetchProducts()
        } else {
            searchInCache()
            remoteSearch()
        }
    }

    private suspend fun fetchProducts(): Result<Unit> {
        return repository.fetchProducts(offset, PAGE_SIZE, productFilters.value).onSuccess {
            canLoadMore = it
            offset += PAGE_SIZE
        }.map { }
    }

    private fun searchInCache() {
        repository.searchProductsInCache(
            offset = offset,
            pageSize = PAGE_SIZE,
            searchQuery = searchQuery.value,
        ).let { loadedProducts ->
            searchResults.update { list -> updateSearchResult(list, loadedProducts) }
        }
    }

    private suspend fun remoteSearch(): Result<Unit> {
        return repository.searchProducts(
            offset = offset,
            pageSize = PAGE_SIZE,
            searchQuery = searchQuery.value,
        ).onSuccess { result ->
            canLoadMore = result.canLoadMore
            offset += PAGE_SIZE
            searchResults.update { list -> updateSearchResult(list, result.products) }
        }.map { }
    }

    private fun updateSearchResult(list: List<Product>, loadedProducts: List<Product>) =
        (list + loadedProducts).distinctBy { it.remoteId }
}
