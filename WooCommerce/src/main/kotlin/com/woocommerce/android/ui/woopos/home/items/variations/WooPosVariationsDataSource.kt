package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.woopos.home.items.common.FetchOptions
import com.woocommerce.android.ui.woopos.home.items.common.WooPosBaseDataSource
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosVariationsDataSource @Inject constructor(
    private val handler: VariationListHandler,
    private val variationCache: VariationsLRUCache<Long, List<ProductVariation>>
) : WooPosBaseDataSource<ProductVariation>() {
    private suspend fun getCachedVariations(productId: Long): List<ProductVariation> {
        return variationCache.get(productId) ?: emptyList()
    }

    private suspend fun updateVariationCache(productId: Long, variations: List<ProductVariation>) {
        variationCache.put(productId, variations)
    }

    suspend fun resetState() {
        handler.resetState()
    }

    fun canLoadMore(numOfVariations: Int): Boolean {
        return handler.canLoadMore(numOfVariations)
    }

    suspend fun loadMore(productId: Long): Result<List<ProductVariation>> = withContext(Dispatchers.IO) {
        val result = handler.loadMore(
            productId,
            filterOptions = mapOf(
                WCProductStore.VariationFilterOption.STATUS to VARIATION_STATUS_PUBLISH,
                WCProductStore.VariationFilterOption.DOWNLOADABLE to VARIATION_DOWNLOADABLE_FALSE
            )
        )
        if (result.isSuccess) {
            val fetchedVariations = handler.getVariationsFlow(productId).first().applyFilter()
            Result.success(fetchedVariations)
        } else {
            result.logFailure()
            Result.failure(
                result.exceptionOrNull() ?: Exception("Unknown error while loading more variations")
            )
        }
    }

    companion object {
        private const val VARIATION_STATUS_PUBLISH = "publish"
        private const val VARIATION_DOWNLOADABLE_FALSE = "false"
    }

    override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<ProductVariation> =
        fetchOptions.productId?.let { productId ->
            getCachedVariations(productId)
        } ?: run {
            throw IllegalArgumentException("Product ID is required to fetch variations from cache")
        }

    override suspend fun fetchFromRemote(
        fetchOptions: FetchOptions
    ): Result<List<ProductVariation>> {
        fetchOptions.productId?.let { productId ->
            return handler.fetchVariations(
                productId,
                forceRefresh = true,
                filterOptions = mapOf(
                    WCProductStore.VariationFilterOption.STATUS to ProductStatus.PUBLISH.value,
                    WCProductStore.VariationFilterOption.DOWNLOADABLE to
                        WCProductStore.DownloadableOptions.FALSE.toString(),
                )
            )
                .mapCatching { handler.getVariationsFlow(productId).firstOrNull()?.applyFilter() ?: emptyList() }
        } ?: run {
            throw IllegalArgumentException("Product ID is required to fetch variations from cache")
        }
    }

    override suspend fun updateCache(fetchOptions: FetchOptions, data: List<ProductVariation>) {
        fetchOptions.productId?.let { productId ->
            updateVariationCache(productId = productId, variations = data)
        } ?: run {
            throw IllegalArgumentException("Product ID is required to fetch variations from cache")
        }
    }
}

private fun Result<Unit>.logFailure() {
    val error = exceptionOrNull()
    val errorMessage = error?.message ?: "Unknown error"
    WooLog.e(WooLog.T.POS, "Loading variations failed - $errorMessage", error)
}

private fun List<ProductVariation>.applyFilter(): List<ProductVariation> {
    return filter { !it.isDownloadable } // Keeping this filter for now, but it should be removed in the future after
    // WC 9.7.0 is released. https://a8c.slack.com/archives/C070SJRA8DP/p1736795937571479
}
