package com.woocommerce.android.ui.woopos.home.products

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
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
class WooPosProductsDataSource @Inject constructor(
    private val handler: ProductListHandler,
    private val productStore: WCProductStore,
    private val site: SelectedSite,
) {
    val hasMorePages: Boolean
        get() = handler.canLoadMore.get()

    fun loadSimpleProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        if (forceRefreshProducts) {
            productStore.deleteProductsForSite(site.get())
        }

        val result = handler.loadFromCacheAndFetch(
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(WCProductStore.ProductFilterOption.TYPE to ProductType.SIMPLE.value)
        )

        emit(
            ProductsResult.Cached(
                handler.productsFlow.first().applyPosProductFilter()
            )
        )

        if (result.isSuccess) {
            val remoteProducts = handler.productsFlow.first().applyPosProductFilter()
            emit(ProductsResult.Remote(Result.success(remoteProducts)))
        } else {
            val error = result.exceptionOrNull()
            val errorMessage = error?.message ?: "Unknown error"
            WooLog.e(WooLog.T.POS, "Loading simple products failed - $errorMessage", error)
            emit(ProductsResult.Remote(Result.failure(result.exceptionOrNull()!!)))
        }
    }.flowOn(Dispatchers.IO).take(2)

    suspend fun loadMore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val result = handler.loadMore()
        if (result.isSuccess) {
            Result.success(handler.productsFlow.first().applyPosProductFilter())
        } else {
            val error = result.exceptionOrNull()
            val errorMessage = error?.message ?: "Unknown error"
            WooLog.e(WooLog.T.POS, "Loading more products failed - $errorMessage", error)
            Result.failure(error!!)
        }
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun List<Product>.applyPosProductFilter() = this.filter { product ->
        isProductPublished(product) &&
            isProductHasAPrice(product) &&
            isProductNotVirtual(product) &&
            isProductNotDownloadable(product)
    }

    private fun isProductNotDownloadable(product: Product) = !product.isDownloadable

    private fun isProductNotVirtual(product: Product) = !product.isVirtual

    private fun isProductHasAPrice(product: Product) =
        (product.price != null && product.price.compareTo(BigDecimal.ZERO) != 0)

    private fun isProductPublished(product: Product) = product.status == ProductStatus.PUBLISH
}
