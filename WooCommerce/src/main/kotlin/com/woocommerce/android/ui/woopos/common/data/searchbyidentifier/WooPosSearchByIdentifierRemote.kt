package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val productStore: WCProductStore,
    private val variationsCache: WooPosVariationsLRUCache,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover,
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat
    ): WooPosSearchByIdentifierResult = coroutineScope {
        val gtinSearchDeferred = async {
            searchAndConvertResult { searchProductsByGlobalUniqueId(identifier) }
        }

        val skuSearchDeferred = async {
            searchAndConvertResult { searchProductsBySku(identifier) }
        }

        val gtinResult = gtinSearchDeferred.await()

        if (gtinResult.isSuccess) {
            skuSearchDeferred.cancel()
            return@coroutineScope gtinResult
        }

        val identifierResult = skuSearchDeferred.await()

        if (identifierResult.isSuccess) {
            return@coroutineScope identifierResult
        }

        val identifierWithoutCheckDigit = checkDigitRemover(identifier, format)
        if (identifierWithoutCheckDigit != identifier) {
            val gtinFallbackDeferred = async {
                searchAndConvertResult { searchProductsByGlobalUniqueId(identifierWithoutCheckDigit) }
            }
            val identifierFallbackDeferred = async {
                searchAndConvertResult { searchProductsBySku(identifierWithoutCheckDigit) }
            }

            val gtinFallbackResult = gtinFallbackDeferred.await()
            if (gtinFallbackResult.isSuccess) {
                identifierFallbackDeferred.cancel()
                return@coroutineScope gtinFallbackResult
            }

            val identifierFallbackResult = identifierFallbackDeferred.await()
            if (identifierFallbackResult.isSuccess) {
                return@coroutineScope identifierFallbackResult
            }

            prioritizeError(gtinFallbackResult, identifierFallbackResult, gtinResult, identifierResult)
        } else {
            prioritizeError(gtinResult, identifierResult)
        }
    }

    private suspend fun searchProductsBySku(identifier: String): Result<List<Product>> {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = identifier,
            skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            filterOptions = emptyMap()
        )

        return when {
            result.isError -> Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.NetworkError))
            result.model != null -> {
                val productSearchResult = result.model!!
                Result.success(productSearchResult.products.map { it.toAppModel() })
            }
            else -> Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
        }
    }

    private suspend fun searchProductsByGlobalUniqueId(globalUniqueId: String): Result<List<Product>> {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = null,
            globalUniqueIdSearchQuery = globalUniqueId,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            filterOptions = emptyMap()
        )

        return when {
            result.isError -> Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.NetworkError))
            result.model != null -> {
                val productSearchResult = result.model!!
                Result.success(productSearchResult.products.map { it.toAppModel() })
            }
            else -> Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
        }
    }

    private suspend fun searchAndConvertResult(
        searchFunction: suspend () -> Result<List<Product>>
    ): WooPosSearchByIdentifierResult {
        val result = searchFunction()

        return when {
            result.isSuccess -> {
                val products = result.getOrThrow()
                val product = products.firstOrNull()
                if (product == null) {
                    return WooPosSearchByIdentifierResult.Failure(
                        WooPosSearchByIdentifierResult.Error.ProductNotFound
                    )
                }

                if (product.type.equals("variation", ignoreCase = true)) {
                    handleVariationResult(product)
                } else {
                    productsCache.addAll(listOf(product))
                    WooPosSearchByIdentifierResult.Success(product)
                }
            }

            else -> handleError(result)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun handleVariationResult(product: Product): WooPosSearchByIdentifierResult = coroutineScope {
        val parentId = product.parentId
        val variationId = product.remoteId

        if (parentId <= 0) {
            return@coroutineScope WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ProductNotFound
            )
        }

        val variationJob = async { getOrFetchVariationAndUpdateCache(variationId, parentId) }
        val parentProductJob = async { getOrFetchProductAndUpdateCache(parentId) }

        val variation = variationJob.await()
        val parentProduct = parentProductJob.await()

        return@coroutineScope if (variation != null && parentProduct != null) {
            WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct)
        } else {
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ProductNotFound
            )
        }
    }

    @Suppress("ReturnCount")
    private fun prioritizeError(
        vararg results: WooPosSearchByIdentifierResult
    ): WooPosSearchByIdentifierResult {
        results.forEach { result ->
            if (result is WooPosSearchByIdentifierResult.Failure &&
                result.error == WooPosSearchByIdentifierResult.Error.RequestCancelled
            ) {
                return result
            }
        }

        results.forEach { result ->
            if (result is WooPosSearchByIdentifierResult.Failure &&
                result.error == WooPosSearchByIdentifierResult.Error.NetworkError
            ) {
                return result
            }
        }

        return WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)
    }

    private fun handleError(result: Result<List<Product>>): WooPosSearchByIdentifierResult {
        val error = result.exceptionOrNull()
        val searchError = when (error) {
            is SearchException -> error.error
            else -> WooPosSearchByIdentifierResult.Error.RequestCancelled
        }
        return WooPosSearchByIdentifierResult.Failure(searchError)
    }

    @Suppress("ReturnCount")
    private suspend fun getOrFetchVariationAndUpdateCache(variationId: Long, parentId: Long): ProductVariation? {
        val cachedVariation = variationsCache.get(variationId)?.find { it.remoteVariationId == variationId }

        if (cachedVariation != null) {
            return cachedVariation
        }
        val variationResult = productStore.fetchSingleVariation(
            selectedSite.get(),
            parentId,
            variationId
        )

        if (variationResult.isError) {
            return null
        }

        return productStore.getVariationByRemoteId(
            selectedSite.get(),
            parentId,
            variationId
        )?.toAppModel()
            ?.also {
                variationsCache.add(parentId, it)
            }
    }

    @Suppress("ReturnCount")
    private suspend fun getOrFetchProductAndUpdateCache(parentProductId: Long): Product? {
        val cachedProduct = productsCache.getProductById(parentProductId)
        if (cachedProduct != null) {
            return cachedProduct
        }

        val parentProductResult = productStore.fetchSingleProduct(
            WCProductStore.FetchSingleProductPayload(
                site = selectedSite.get(),
                remoteProductId = parentProductId,
            )
        )

        if (parentProductResult.isError) {
            return null
        }

        return productStore.getProduct(selectedSite.get(), parentProductId)
            ?.toAppModel()
            ?.also { product ->
                productsCache.addAll(listOf(product))
            }
    }
}

internal class SearchException(val error: WooPosSearchByIdentifierResult.Error) : Exception()
