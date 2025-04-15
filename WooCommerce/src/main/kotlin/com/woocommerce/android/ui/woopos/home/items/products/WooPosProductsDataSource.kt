package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 25 // Matches [ProductListHandler]'s PAGE_SIZE

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val handler: ProductListHandler,
    private val productsCache: WooPosProductsCache,
    private val productsIndex: WooPosProductsIndex,
) {
    val hasMorePages: Boolean
        get() = handler.canLoadMore.get()

    fun loadSimpleProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        if (forceRefreshProducts) {
            productsCache.clear()
        }
        productsIndex.clearCache()

        val cachedProducts = productsIndex.getProductList().take(PAGE_SIZE)
        emit(ProductsResult.Cached(sortProductsByName(cachedProducts)))

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
            productsCache.addAll(remoteProducts)
            productsIndex.storeProductList(remoteProducts.map { it.remoteId })
            emit(ProductsResult.Remote(Result.success(sortProductsByName(productsIndex.getProductList()))))
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
            productsCache.addAll(moreProducts)
            productsIndex.storeProductList(moreProducts.map { it.remoteId })
            Result.success(sortProductsByName(productsIndex.getProductList()))
        } else {
            result.logFailure()
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    private fun sortProductsByName(products: List<Product>): List<Product> {
        return products.sortedBy { it.name }
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
}
