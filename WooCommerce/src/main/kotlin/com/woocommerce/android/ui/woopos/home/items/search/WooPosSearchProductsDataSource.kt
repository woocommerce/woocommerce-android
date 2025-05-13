package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSearchProductsDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val searchResultsIndex: WooPosSearchResultsIndex,
    private val searchPredicate: WooPosProductSearchPredicate,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig
) {
    companion object {
        private const val PAGE_SIZE = 15
    }

    private val canLoadMore = AtomicBoolean(false)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    suspend fun searchLocalProducts(query: String): List<Product> = withContext(Dispatchers.IO) {
        sortProducts(productsCache.getAll().filter(searchPredicate(query))).take(PAGE_SIZE)
    }

    suspend fun searchRemoteProducts(query: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        searchResultsIndex.clearCache()

        performRemoteSearch(query).fold(
            onSuccess = { result -> Result.success(result.products) },
            onFailure = { error -> Result.failure(error) }
        )
    }

    suspend fun loadMore(query: String): Result<List<Product>> {
        if (!canLoadMore.get()) {
            return Result.success(searchResultsIndex.getSearchResults(query))
        }

        val currentResults = searchResultsIndex.getSearchResults(query)
        val offset = currentResults.size

        return performRemoteSearch(query, offset).fold(
            onSuccess = { result ->
                Result.success(searchResultsIndex.getSearchResults(query))
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private suspend fun performRemoteSearch(
        searchQuery: String,
        offset: Int = 0
    ): Result<SearchResult> {
        return productStore.searchProductsByNameAndSku(
            selectedSite.get(),
            searchNameOrSkuQuery = searchQuery,
            offset = offset,
            pageSize = PAGE_SIZE,
            filterOptions = productsTypesFilterConfig.filters,
            includeTypes = productsTypesFilterConfig.includeTypes,
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.POS,
                    "Searching products failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                val products = searchResult.products.map { product -> product.toAppModel() }
                canLoadMore.set(searchResult.canLoadMore)
                productsCache.addAll(products)
                searchResultsIndex.storeSearchResults(
                    searchQuery,
                    products.map { it.remoteId }
                )
                val searchResults = SearchResult(
                    products = searchResultsIndex.getSearchResults(searchQuery)
                        .sortedBy { it.name.lowercase() },
                    canLoadMore = searchResult.canLoadMore
                )
                Result.success(searchResults)
            }
        }
    }

    private fun sortProducts(products: List<Product>): List<Product> {
        return products.sortedBy { it.name.lowercase() }
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    data class SearchResult(
        val products: List<Product>,
        val canLoadMore: Boolean
    )
}
