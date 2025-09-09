package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
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
    private val productMapper: WCProductToWooPosProductModelMapper,
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 10
    }

    private val mutex = Mutex()
    private val popularProductsCache = mutableListOf<WooPosProductModel>()

    suspend fun getPopularProducts(): List<WooPosProductModel> = mutex.withLock { popularProductsCache }

    suspend fun addPopularItemsToCache() = mutex.withLock {
        productsCache.addAll(popularProductsCache)
    }

    suspend fun fetchAndCachePopularProducts(): Result<Unit> = mutex.withLock {
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
            val productsAppModel = products.map { productMapper.map(it) }

            popularProductsCache.clear()
            popularProductsCache.addAll(productsAppModel)
            productsCache.addAll(productsAppModel)
            Result.success(Unit)
        }
    }
}
