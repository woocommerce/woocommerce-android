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

    /**
     * Refreshes products data.
     * For local catalog sources, performs incremental sync of both products and variations if supported.
     * For remote sources, performs a regular refresh of products.
     */
    suspend fun refreshProducts(): Flow<WooPosProductsDataSource.ProductsResult>

    fun fetchFirstVariationsPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.VariationsResult>

    suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>>

    fun canLoadMoreVariations(numOfVariations: Int): Boolean

    /**
     * Refreshes variations data.
     * For local catalog sources, performs incremental sync of both products and variations if supported.
     * For remote sources, performs a regular refresh of variations.
     */
    suspend fun refreshVariations(productId: Long): Flow<WooPosProductsDataSource.VariationsResult>
}
