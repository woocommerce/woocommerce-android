package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosProductsDataSourceImpl @Inject constructor(private val handler: ProductListHandler) :
    WooPosProductsDataSource {
    override val products: Flow<List<Product>> = handler.productsFlow

    override suspend fun loadSimpleProducts() {
        handler.loadFromCacheAndFetch(
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(WCProductStore.ProductFilterOption.TYPE to ProductType.SIMPLE.value)
        )
    }

    override suspend fun loadMore() {
        handler.loadMore()
    }
}
