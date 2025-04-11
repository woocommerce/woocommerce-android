package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache
) {
    private val canLoadMore = AtomicBoolean(false)
    private val offset = AtomicInteger(0)
    private val pageSize = 25

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    fun loadProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        offset.set(0)
        if (forceRefreshProducts) {
            productsCache.clear()
        }
        val cachedProducts = productsCache.getAll()
        emit(ProductsResult.Cached(sortProductsByName(cachedProducts)))

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            emit(ProductsResult.Remote(Result.success(sortProductsByName(productsCache.getAll()))))
        } else {
            emit(ProductsResult.Remote(Result.failure(fetchResult.exceptionOrNull() ?: Exception("Unknown error"))))
        }
    }.flowOn(Dispatchers.IO).take(2)

    suspend fun loadMore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        if (!canLoadMore.get()) {
            return@withContext Result.success(sortProductsByName(productsCache.getAll()))
        }

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            Result.success(sortProductsByName(productsCache.getAll()))
        } else {
            fetchResult
        }
    }

    private suspend fun fetchProducts(): Result<List<Product>> {
        val result = productStore.fetchProducts(
            site = selectedSite.get(),
            offset = offset.get(),
            pageSize = pageSize,
            filterOptions = createProductFilters(),
            includeTypes = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable),
        )

        return if (!result.isError) {
            val productsList = result.model ?: emptyList()
            val products = productsList.map { it.toAppModel() }

            canLoadMore.set(productsList.size == pageSize)
            offset.addAndGet(pageSize)

            productsCache.addAll(products)
            Result.success(sortProductsByName(productsCache.getAll()))
        } else {
            result.logFailure()
            Result.failure(WooException(result.error))
        }
    }

    private fun createProductFilters(): Map<ProductFilterOption, String> {
        return mapOf(
            ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
            ProductFilterOption.DOWNLOADABLE to DownloadableOptions.FALSE.toString()
        )
    }

    private fun sortProductsByName(products: List<Product>): List<Product> {
        return products.sortedBy { it.name }
    }

    private fun WooResult<*>.logFailure() {
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage")
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }
}
