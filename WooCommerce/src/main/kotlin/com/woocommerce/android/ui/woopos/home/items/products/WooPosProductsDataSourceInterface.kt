package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import kotlinx.coroutines.flow.Flow

interface WooPosProductsDataSourceInterface {
    fun fetchFirstPage(
        searchQuery: String? = null,
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.ProductsResult>

    suspend fun loadMore(): Result<List<WooPosProductModel>>

    val hasMorePages: Boolean

    suspend fun resetState()
}
