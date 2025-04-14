package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
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
    private val searchResultsPaginationHelper: WooPosSearchResultsPaginationHelper,
    private val searchPredicate: ProductSearchPredicate,
) {
    companion object {
        private const val PAGE_SIZE = 100
    }

    private val canLoadMore = AtomicBoolean(false)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    fun searchProducts(query: String): Flow<ProductsResult> = flow {
        coroutineScope {
            searchResultsPaginationHelper.clearCache()

            val localSearchDeferred = async { productsCache.getAll().filter(searchPredicate(query)) }
            val remoteSearchDeferred = async { remoteSearch(query) }

            val localResults = localSearchDeferred.await()
            if (localResults.isNotEmpty()) {
                emit(ProductsResult.Cached(localResults))
            }

            val remoteResults = remoteSearchDeferred.await()
            remoteResults.fold(
                onSuccess = { result ->
                    canLoadMore.set(result.canLoadMore)
                    productsCache.addAll(result.products)
                    searchResultsPaginationHelper.storeSearchResults(query, result.products.map { it.remoteId })
                    emit(ProductsResult.Remote(Result.success(result.products)))
                },
                onFailure = { error ->
                    if (localResults.isNotEmpty()) {
                        searchResultsPaginationHelper.storeSearchResults(query, localResults.map { it.remoteId })
                    }
                    emit(ProductsResult.Remote(Result.failure(error)))
                }
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadMore(query: String): Result<List<Product>> {
        if (!canLoadMore.get()) {
            return Result.success(searchResultsPaginationHelper.getSearchResults(query))
        }

        val currentResults = searchResultsPaginationHelper.getSearchResults(query)
        val offset = currentResults.size

        return remoteSearch(query, offset).fold(
            onSuccess = { result ->
                canLoadMore.set(result.canLoadMore)
                productsCache.addAll(result.products)
                searchResultsPaginationHelper.storeSearchResults(query, result.products.map { it.remoteId })
                Result.success(searchResultsPaginationHelper.getSearchResults(query))
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun getProductById(productId: Long): Product? = productsCache.getProductById(productId)

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

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    data class SearchResult(
        val products: List<Product>,
        val canLoadMore: Boolean
    )
}
