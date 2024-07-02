package com.woocommerce.android.ui.products.selector

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.SkuSearchOptions
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class ProductListHandler @Inject constructor(private val repository: ProductSelectorRepository) {
    companion object {
        private const val PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private val offset = MutableStateFlow(0)
    val canLoadMore = AtomicBoolean(true)

    private val searchQuery = MutableStateFlow("")
    private val searchType = MutableStateFlow(SearchType.DEFAULT)
    private val searchResults = MutableStateFlow(emptyList<Product>())

    private val productFilters = MutableStateFlow(mapOf<ProductFilterOption, String>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsFlow: Flow<List<Product>> = combine(searchQuery, productFilters, offset) { query, filters, offset ->
        if (query.isEmpty()) {
            repository.observeProducts(filters).map {
                it.take(if (offset == 0) PAGE_SIZE else offset)
            }
        } else {
            searchResults
        }
    }.flatMapLatest { it }

    suspend fun loadFromCacheAndFetch(
        searchQuery: String = "",
        filters: Map<ProductFilterOption, String> = emptyMap(),
        searchType: SearchType,
    ): Result<Unit> = mutex.withLock {
        offset.value = 0
        searchResults.value = emptyList()

        this.searchQuery.value = searchQuery
        this.searchType.value = searchType
        this.productFilters.value = filters

        return if (searchQuery.isNotEmpty()) {
            when (searchType) {
                SearchType.DEFAULT -> {
                    searchInCache()
                    remoteSearch()
                }
                SearchType.SKU -> {
                    searchInCache()
                    remoteSearch()
                }
            }
        } else {
            fetchProducts()
        }
    }

    // The implementation of loadMore has limited functionality. Essentially, more items from local cache are loaded
    // only after the remote request to fetch the previous page finishes successfully.
    suspend fun loadMore() = mutex.withLock {
        if (!canLoadMore.get()) return@withLock Result.success(Unit)
        if (searchQuery.value.isEmpty()) {
            fetchProducts()
        } else {
            searchInCache()
            remoteSearch()
        }
    }

    private suspend fun fetchProducts(): Result<Unit> {
        return repository.fetchProducts(offset.value, PAGE_SIZE, productFilters.value).onSuccess {
            canLoadMore.set(it)
            offset.value += PAGE_SIZE
        }.map { }
    }

    private fun searchInCache() {
        val searchOptions = if (searchType.value == SearchType.SKU) {
            SkuSearchOptions.PartialMatch
        } else {
            SkuSearchOptions.Disabled
        }
        repository.searchProductsInCache(
            offset = offset.value,
            pageSize = PAGE_SIZE,
            searchQuery = searchQuery.value,
            skuSearchOptions = searchOptions
        ).let { loadedProducts ->
            searchResults.update { list -> updateSearchResult(list, loadedProducts) }
        }
    }

    private suspend fun remoteSearch(): Result<Unit> {
        val searchOptions = if (searchType.value == SearchType.SKU) {
            SkuSearchOptions.PartialMatch
        } else {
            SkuSearchOptions.Disabled
        }
        return repository.searchProducts(
            offset = offset.value,
            pageSize = PAGE_SIZE,
            searchQuery = searchQuery.value,
            skuSearchOption = searchOptions
        ).onSuccess { result ->
            canLoadMore.set(result.canLoadMore)
            offset.value += PAGE_SIZE
            searchResults.update { list -> updateSearchResult(list, result.products) }
        }.map { }
    }

    private fun updateSearchResult(list: List<Product>, loadedProducts: List<Product>) =
        (list + loadedProducts).distinctBy { it.remoteId }

    enum class SearchType(@StringRes val labelResId: Int) {
        DEFAULT(R.string.product_search_all), SKU(R.string.product_search_sku);

        companion object {
            fun fromLabelResId(@StringRes labelResId: Int) = values().firstOrNull { it.labelResId == labelResId }
        }
    }
}
