package com.woocommerce.android.ui.woopos.home.products

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(private val handler: ProductListHandler) {
    private val productCache = mutableListOf<Product>()

    val hasMorePages: Boolean
        get() = handler.canLoadMore.get()

    fun loadSimpleProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        if (forceRefreshProducts) {
            productCache.clear()
        }

        emit(ProductsResult.Cached(productCache))

        val result = handler.loadFromCacheAndFetch(
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(
                WCProductStore.ProductFilterOption.TYPE to ProductType.SIMPLE.value,
                WCProductStore.ProductFilterOption.STATUS to ProductStatus.PUBLISH.value
            )
        )

        if (result.isSuccess) {
            val remoteProducts = handler.productsFlow.first().applyPosProductFilter()
            productCache.clear()
            productCache.addAll(remoteProducts)
            emit(ProductsResult.Remote(Result.success(remoteProducts)))
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
        val result = handler.loadMore()
        if (result.isSuccess) {
            val moreProducts = handler.productsFlow.first().applyPosProductFilter()
            productCache.addAll(moreProducts)
            Result.success(moreProducts)
        } else {
            result.logFailure()
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun List<Product>.applyPosProductFilter() = this.filter { product ->
            isProductHasAPrice(product) &&
            isProductNotVirtual(product) &&
            isProductNotDownloadable(product)
    }

    private fun isProductNotDownloadable(product: Product) = !product.isDownloadable

    private fun isProductNotVirtual(product: Product) = !product.isVirtual

    private fun isProductHasAPrice(product: Product) =
        (product.price != null && product.price.compareTo(BigDecimal.ZERO) != 0)
}
