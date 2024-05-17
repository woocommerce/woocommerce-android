package com.woocommerce.android.ui.woopos.cart.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.selector.ProductListHandler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductsDataSourceImpl @Inject constructor(private val handler: ProductListHandler) : ProductsDataSource {
    override val products: Flow<List<Product>> = handler.productsFlow

    override suspend fun loadProducts() {
         handler.loadFromCacheAndFetch(searchType = ProductListHandler.SearchType.DEFAULT)
    }

    override suspend fun loadMore() {
         handler.loadMore()
    }
}