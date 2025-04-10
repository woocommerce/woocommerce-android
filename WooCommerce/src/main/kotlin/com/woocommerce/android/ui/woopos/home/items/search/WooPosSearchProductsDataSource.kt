package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSearchProductsDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PAGE_SIZE = 25
        private const val MAX_CACHE_SIZE = 1000
    }

    private val canLoadMore = AtomicBoolean(true)
    private val searchResultsCache =
        object : LinkedHashMap<String, List<Product>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, List<Product>>): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    fun searchProducts(query: String): Flow<ProductsResult> = flow {
        val cachedResults = getCachedSearchResults(query)
        emit(ProductsResult.Cached(cachedResults))

        val remoteResults = remoteSearch(query)
        remoteResults.fold(
            onSuccess = { result ->
                canLoadMore.set(result.canLoadMore)
                updateSearchResultsCache(query, result.products)
                emit(ProductsResult.Remote(Result.success(result.products)))
            },
            onFailure = { error ->
                emit(ProductsResult.Remote(Result.failure(error)))
            }
        )
    }.flowOn(Dispatchers.IO)

    private fun getCachedSearchResults(query: String): List<Product> {
        return searchResultsCache[query.lowercase()] ?: emptyList()
    }

    suspend fun loadMore(query: String): Result<List<Product>> {
        if (!canLoadMore.get()) {
            return Result.success(getCachedSearchResults(query))
        }

        val currentResults = searchResultsCache[query.lowercase()] ?: emptyList()
        val offset = currentResults.size

        return remoteSearch(query, offset).fold(
            onSuccess = { result ->
                canLoadMore.set(result.canLoadMore)
                val combinedResults = currentResults + result.products
                updateSearchResultsCache(query, combinedResults)
                Result.success(combinedResults)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    fun getProductById(productId: Long): Product? =
        searchResultsCache.values.flatten().find { it.remoteId == productId }

    private suspend fun remoteSearch(
        searchQuery: String,
        offset: Int = 0
    ): Result<SearchResult> {
        return productStore.searchProducts(
            selectedSite.get(),
            searchString = searchQuery,
            offset = offset,
            pageSize = PAGE_SIZE,
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.POS,
                    "Searching products failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                Result.success(
                    SearchResult(
                        products = searchResult.products.map { product -> product.toAppModel() },
                        canLoadMore = searchResult.canLoadMore
                    )
                )
            }
        }
    }

    private fun updateSearchResultsCache(query: String, results: List<Product>) {
        searchResultsCache[query.lowercase()] = results
    }

    fun getPopularProducts(): List<Product> {
        return searchResultsCache.entries.firstOrNull()?.value?.shuffled() ?: emptyList()
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
