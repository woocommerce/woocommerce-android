package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.Companion.NORMAL_PAGE_SIZE
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsIndex
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosMockedPopularProductsProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 3
    }

    suspend fun getPopularProducts(): List<Product> {
        val result = productStore.fetchProducts(
            site = selectedSite.get(),
            offset = 0,
            pageSize = MAX_POPULAR_PRODUCTS,
            filterOptions = createProductFilters(),
            includeTypes = createIncludedTypes(),
            sortType = ProductSorting.POPULARITY_DESC,
        )


    }
}
