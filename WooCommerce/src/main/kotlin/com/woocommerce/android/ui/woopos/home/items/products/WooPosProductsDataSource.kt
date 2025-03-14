package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.home.items.common.FetchOptions
import com.woocommerce.android.ui.woopos.home.items.common.WooPosBaseDataSource
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val handler: ProductListHandler,
) : WooPosBaseDataSource<Product>() {
    private var productCache: List<Product> = emptyList()
    private val cacheMutex = Mutex()

    val hasMorePages: Boolean
        get() = handler.canLoadMore.get()

    suspend fun loadMore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val result = handler.loadMore(
            includeTypes = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable),
        )
        if (result.isSuccess) {
            val moreProducts = handler.productsFlow.first()
            updateProductCache(moreProducts)
            Result.success(productCache)
        } else {
            result.logFailure()
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    private suspend fun updateProductCache(newList: List<Product>) {
        cacheMutex.withLock { productCache = newList }
    }

    private fun Result<Unit>.logFailure() {
        val error = exceptionOrNull()
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage", error)
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<Product> {
        return productCache
    }

    override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<Product>> {
        val result = handler.loadFromCacheAndFetch(
            forceRefresh = true,
            includeType = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable),
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(
                WCProductStore.ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
                WCProductStore.ProductFilterOption.DOWNLOADABLE to WCProductStore.DownloadableOptions.FALSE.toString(),
            )
        )
        return if (result.isSuccess) {
            Result.success(handler.productsFlow.first())
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error while fetching products"))
        }
    }

    override suspend fun updateCache(fetchOptions: FetchOptions, data: List<Product>) {
        updateProductCache(data)
    }
}
