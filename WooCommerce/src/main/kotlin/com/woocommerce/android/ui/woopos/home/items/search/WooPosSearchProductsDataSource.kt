package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
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
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig,
    private val posProductModelMapper: WCProductToWooPosProductModelMapper,
) {
    companion object {
        private const val PAGE_SIZE = 15
        private val SEARCH_FIELDS = listOf("name", "sku", "global_unique_id")
    }

    private val canLoadMore = AtomicBoolean(false)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    suspend fun searchLocalProducts(query: String): List<WooPosProductModel> = withContext(Dispatchers.IO) {
        sortProducts(productsCache.getAll().filter(searchPredicate(query))).take(PAGE_SIZE)
    }

    suspend fun searchRemoteProducts(query: String): Result<List<WooPosProductModel>> =
        withContext(Dispatchers.IO) {
            searchResultsIndex.clearCache()

            performRemoteSearch(query).fold(
                onSuccess = { result ->
                    Result.success(result.products.sortedBy { it.name.lowercase() })
                },
                onFailure = { error -> Result.failure(error) }
            )
        }

    suspend fun loadMore(query: String): Result<List<WooPosProductModel>> {
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
            searchFields = SEARCH_FIELDS,
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.POS,
                    "Searching products failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                val products = searchResult.products
                    .map { product -> posProductModelMapper.map(product) }
                    .sortedBy { it.name.lowercase() }

                canLoadMore.set(searchResult.canLoadMore)
                productsCache.addAll(products)
                searchResultsIndex.storeSearchResults(
                    searchQuery,
                    products.map { it.remoteId }
                )
                val searchResults = SearchResult(
                    products = searchResultsIndex.getSearchResults(searchQuery),
                    canLoadMore = searchResult.canLoadMore
                )
                Result.success(searchResults)
            }
        }
    }

    private fun sortProducts(products: List<WooPosProductModel>): List<WooPosProductModel> {
        return products.sortedBy { it.name.lowercase() }
    }

    sealed class ProductsResult {
        data class Cached(val products: List<WooPosProductModel>) : ProductsResult()
        data class Remote(val productsResult: Result<List<WooPosProductModel>>) : ProductsResult()
    }

    data class SearchResult(
        val products: List<WooPosProductModel>,
        val canLoadMore: Boolean
    )
}
