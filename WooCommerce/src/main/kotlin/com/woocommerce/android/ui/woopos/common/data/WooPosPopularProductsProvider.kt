package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosPopularProductsProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val productsCache: WooPosProductsCache,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig,
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 3
    }

    private val mutex = Mutex()
    private val popularProductsCache = mutableListOf<Product>()

    suspend fun getPopularProducts(): List<Product> = mutex.withLock { popularProductsCache }

    suspend fun fetchPopularProducts(): Result<Unit> = mutex.withLock {
        val result = productStore.fetchProducts(
            site = selectedSite.get(),
            offset = 0,
            pageSize = MAX_POPULAR_PRODUCTS,
            filterOptions = productsTypesFilterConfig.filters,
            includeTypes = productsTypesFilterConfig.includeTypes,
            sortType = ProductSorting.POPULARITY_DESC,
        )

        return if (result.isError) {
            Result.failure(Exception(result.error.message))
        } else {
            val products = result.model ?: emptyList()
            var productsAppModel = products.map { it.toAppModel() }

            popularProductsCache.clear()
            popularProductsCache.addAll(productsAppModel)
            productsCache.addAll(productsAppModel)
            Result.success(Unit)
        }
    }
}
