package com.woocommerce.android.ui.woopos.cartcheckout.products

import com.woocommerce.android.model.Product
import kotlinx.coroutines.flow.Flow

interface WooPosProductsDataSource {
    val products: Flow<List<Product>>
    suspend fun loadProducts()
    suspend fun loadMore()
}
