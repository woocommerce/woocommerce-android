package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import kotlinx.coroutines.flow.Flow

interface WooPosProductsDataSourceInterface {
    fun fetchFirstPage(
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.ProductsResult>

    suspend fun loadMore(): Result<List<WooPosProductModel>>

    val hasMorePages: Boolean

    suspend fun resetVariationsListHandler()

    suspend fun prepopulateProductsCache(): Result<Unit>

    fun fetchVariationsFirstPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.VariationsResult>

    suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>>

    fun canLoadMoreVariations(numOfVariations: Int): Boolean
}
