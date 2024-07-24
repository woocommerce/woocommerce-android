package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler
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
                handler.productsFlow.first().filter { product -> product.price != null }
            )
        )

        if (result.isSuccess) {
            val remoteProducts = handler.productsFlow.first().filter { it.price != null }
            emit(ProductsResult.Remote(Result.success(remoteProducts)))
        } else {
            emit(ProductsResult.Remote(Result.failure(result.exceptionOrNull()!!)))
        }
    }.flowOn(Dispatchers.IO).take(2)

    suspend fun loadMore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val result = handler.loadMore()
        if (result.isSuccess) {
            Result.success(handler.productsFlow.first().filter { it.price != null })
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }
}
