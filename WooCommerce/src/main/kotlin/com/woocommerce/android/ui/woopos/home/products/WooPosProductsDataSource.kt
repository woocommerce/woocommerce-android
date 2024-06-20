package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import kotlinx.coroutines.flow.Flow

interface WooPosProductsDataSource {
    val products: Flow<List<Product>>
    suspend fun loadSimpleProducts()
    suspend fun loadMore()
}
