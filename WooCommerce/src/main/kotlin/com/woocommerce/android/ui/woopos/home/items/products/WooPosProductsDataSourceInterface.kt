package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncVariationResult
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId

sealed class SearchProductsResult {
    data class Local(
        val products: List<WooPosProductModel>,
        val searchTimeMillis: Long,
        val searchMethod: String,
    ) : SearchProductsResult()
    data class Remote(
        val productsResult: Result<List<WooPosProductModel>>,
        val searchTimeMillis: Long,
    ) : SearchProductsResult()
}

interface WooPosProductsDataSourceInterface {
    fun fetchFirstProductsPage(
        forceRefresh: Boolean
    ): Flow<ProductsResult>

    suspend fun loadMoreProducts(): Result<List<WooPosProductModel>>

    val hasMoreProductsPages: Boolean

    suspend fun getProductById(productId: LocalOrRemoteId.RemoteId): WooPosProductModel?

    suspend fun resetVariationsListHandler()

    suspend fun prepopulateCache(): Result<Unit>

    fun fetchFirstVariationsPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<VariationsResult>

    suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>>

    fun canLoadMoreVariations(numOfVariations: Int): Boolean

    suspend fun getVariationById(productId: Long, variationId: Long): WooPosVariation?

    suspend fun refreshProducts(): Result<WooPosSyncProductResult>

    suspend fun refreshVariations(productId: Long): Result<WooPosSyncVariationResult>

    fun searchProducts(query: String): Flow<SearchProductsResult>

    suspend fun loadMoreSearchResults(query: String): Result<List<WooPosProductModel>>

    val hasMoreSearchPages: Boolean
}
