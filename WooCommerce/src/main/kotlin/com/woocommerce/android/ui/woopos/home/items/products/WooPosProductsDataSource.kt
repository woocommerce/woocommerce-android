package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.home.items.common.BaseDataSource
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val handler: ProductListHandler,
) : BaseDataSource<Product>() {
    private var productCache: List<Product> = emptyList()
    private val cacheMutex = Mutex()

    val hasMorePages: Boolean
        get() = handler.canLoadMore.get()

    fun loadSimpleProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        if (forceRefreshProducts) {
            updateProductCache(emptyList())
        }

        emit(ProductsResult.Cached(productCache))

        val result = handler.loadFromCacheAndFetch(
            forceRefresh = forceRefreshProducts,
            searchType = ProductListHandler.SearchType.DEFAULT,
            includeType = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable),
            filters = mapOf(
                WCProductStore.ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
                WCProductStore.ProductFilterOption.DOWNLOADABLE to WCProductStore.DownloadableOptions.FALSE.toString(),
            )
        )

        if (result.isSuccess) {
            val remoteProducts = handler.productsFlow.first()
            updateProductCache(remoteProducts)
            emit(ProductsResult.Remote(Result.success(productCache)))
        } else {
            result.logFailure()
            emit(
                ProductsResult.Remote(
                    Result.failure(
                        result.exceptionOrNull() ?: Exception("Unknown error")
                    )
                )
            )
        }
    }.flowOn(Dispatchers.IO).take(2)

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

    override suspend fun fetchFromCache(productId: Long?): List<Product> {
        return productCache
    }

    override suspend fun fetchFromRemote(
        productId: Long?
    ): Result<List<Product>> {
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

    override suspend fun updateCache(
        productId: Long?,
        data: List<Product>
    ) {
        updateProductCache(data)
    }
}
