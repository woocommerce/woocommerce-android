package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val searchResultsIndex: WooPosSearchResultsIndex,
    private val searchPredicate: ProductSearchPredicate,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig
) {
    companion object {
        private const val PAGE_SIZE = 15
    }

    private val canLoadMore = AtomicBoolean(false)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    fun searchProducts(query: String): Flow<ProductsResult> = flow {
        coroutineScope {
            searchResultsIndex.clearCache()

            val localSearchDeferred = async {
                productsCache.getAll().filter(searchPredicate(query)).sortedBy { it.name }.take(PAGE_SIZE)
            }
            val remoteSearchDeferred = async { performRemoteSearch(query) }

            emit(ProductsResult.Cached(localSearchDeferred.await()))

            val remoteResults = remoteSearchDeferred.await()
            remoteResults.fold(
                onSuccess = { result ->
                    emit(ProductsResult.Remote(Result.success(result.products)))
                },
                onFailure = { error ->
                    emit(ProductsResult.Remote(Result.failure(error)))
                }
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadMore(query: String): Result<List<Product>> {
        if (!canLoadMore.get()) {
            return Result.success(searchResultsIndex.getSearchResults(query))
        }

        val currentResults = searchResultsIndex.getSearchResults(query)
        val offset = currentResults.size

        return performRemoteSearch(query, offset).fold(
            onSuccess = { result ->
                Result.success(result.products)
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
        return productStore.searchProducts(
            selectedSite.get(),
            searchString = searchQuery,
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
                        .sortedBy { it.name },
                    canLoadMore = searchResult.canLoadMore
                )

                Result.success(searchResults)
            }
        }
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
