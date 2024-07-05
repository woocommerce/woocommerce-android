package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val handler: ProductListHandler,
    private val productStore: WCProductStore,
    private val site: SelectedSite,
) {
    val hasMorePages = handler.canLoadMore.get()

    val products: Flow<List<Product>> = handler.productsFlow
        .map { it.filter { product -> product.price != null } }

    suspend fun loadSimpleProducts(forceRefreshProducts: Boolean = false): Result<Unit> {
        if (forceRefreshProducts) {
            productStore.deleteProductsForSite(site.getOrNull()!!)
        }
        return handler.loadFromCacheAndFetch(
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(WCProductStore.ProductFilterOption.TYPE to ProductType.SIMPLE.value),
        )
    }

    suspend fun loadMore() = handler.loadMore()
}
