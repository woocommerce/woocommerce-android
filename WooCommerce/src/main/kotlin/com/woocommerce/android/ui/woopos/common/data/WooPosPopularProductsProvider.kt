package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosPopularProductsProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig,
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 3
    }

    private val popularProductsCache = mutableListOf<Product>()

    fun getPopularProducts(): List<Product> = popularProductsCache

    suspend fun fetchPopularProducts(): Result<Unit> {
        val result = productStore.fetchProducts(
            site = selectedSite.get(),
            offset = 0,
            pageSize = MAX_POPULAR_PRODUCTS,
            filterOptions = productsTypesFilterConfig.filters,
            includeTypes = productsTypesFilterConfig.includeTypes,
            sortType = ProductSorting.POPULARITY_DESC,
        )

        if (result.isError) {
            return Result.failure(Exception(result.error.message))
        } else {
            val products = result.model ?: emptyList()
            popularProductsCache.clear()
            popularProductsCache.addAll(products.map { it.toAppModel() })
            return Result.success(Unit)
        }
    }
}
