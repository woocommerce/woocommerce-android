package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val productsIndex: WooPosProductsIndex,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig
) {
    private val canLoadMore = AtomicBoolean(false)
    private val offset = AtomicInteger(0)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    suspend fun prepopulateProductsCache(): Result<Unit> {
        productsCache.clear()

        var currentPage = 0
        val productsToFetch = mutableListOf<Product>()
        var hasMoreToFetch = true

        while (hasMoreToFetch && currentPage < PRE_POPULATION_MAX_PAGES) {
            val result = productStore.fetchProducts(
                site = selectedSite.get(),
                offset = currentPage * PRE_POPULATION_PAGE_SIZE,
                pageSize = PRE_POPULATION_PAGE_SIZE,
                filterOptions = productsTypesFilterConfig.filters,
                includeTypes = productsTypesFilterConfig.includeTypes,
                sortType = WCProductStore.DEFAULT_PRODUCT_SORTING,
            )

            if (!result.isError) {
                val productsList = result.model ?: emptyList()
                val products = productsList.map { it.toAppModel() }

                productsToFetch.addAll(products)

                hasMoreToFetch = products.size == PRE_POPULATION_PAGE_SIZE
                currentPage++
            } else {
                result.logFailure()
                return Result.failure(WooException(result.error))
            }
        }

        productsCache.addAll(productsToFetch)
        return Result.success(Unit)
    }

    fun loadProducts(forceRefreshProducts: Boolean): Flow<ProductsResult> = flow {
        offset.set(0)
        if (forceRefreshProducts) {
            productsCache.clear()
        }
        productsIndex.clearCache()

        val cachedProducts = sortProducts(productsCache.getAll()).take(NORMAL_PAGE_SIZE)
        emit(ProductsResult.Cached(cachedProducts))

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            emit(ProductsResult.Remote(Result.success(fetchResult.getOrThrow())))
        } else {
            emit(ProductsResult.Remote(Result.failure(fetchResult.exceptionOrNull() ?: Exception("Unknown error"))))
        }
    }.flowOn(Dispatchers.IO).take(2)

    suspend fun loadMore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        if (!canLoadMore.get()) {
            return@withContext Result.success(productsIndex.getProductList())
        }

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            Result.success(fetchResult.getOrThrow())
        } else {
            fetchResult
        }
    }

    private fun sortProducts(products: List<Product>): List<Product> {
        return products.sortedBy { it.name.lowercase() }
    }

    private suspend fun fetchProducts(): Result<List<Product>> {
        val result = productStore.fetchProducts(
            site = selectedSite.get(),
            offset = offset.get(),
            pageSize = NORMAL_PAGE_SIZE,
            filterOptions = productsTypesFilterConfig.filters,
            includeTypes = productsTypesFilterConfig.includeTypes,
            sortType = WCProductStore.DEFAULT_PRODUCT_SORTING,
        )

        return if (!result.isError) {
            val productsList = result.model ?: emptyList()
            val products = productsList.map { it.toAppModel() }

            canLoadMore.set(productsList.size == NORMAL_PAGE_SIZE)
            offset.addAndGet(NORMAL_PAGE_SIZE)

            productsCache.addAll(products)
            productsIndex.storeProductList(products.map { it.remoteId })
            Result.success(productsIndex.getProductList())
        } else {
            result.logFailure()
            Result.failure(WooException(result.error))
        }
    }

    private fun WooResult<*>.logFailure() {
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage")
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    companion object {
        private const val NORMAL_PAGE_SIZE = 25
        private const val PRE_POPULATION_PAGE_SIZE = 100
        private const val PRE_POPULATION_MAX_PAGES = 2
    }
}
