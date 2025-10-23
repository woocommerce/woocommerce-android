package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import kotlinx.coroutines.flow.Flow

interface WooPosProductsDataSourceInterface {
    fun fetchFirstProductsPage(
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.ProductsResult>

    suspend fun loadMoreProducts(): Result<List<WooPosProductModel>>

    val hasMoreProductsPages: Boolean

    suspend fun resetVariationsListHandler()

    suspend fun prepopulateCache(): Result<Unit>

    fun fetchFirstVariationsPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.VariationsResult>

    suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>>

    fun canLoadMoreVariations(numOfVariations: Int): Boolean
}
