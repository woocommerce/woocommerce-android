package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.AppConstants
import com.woocommerce.android.di.LimitedConcurrencyDispatcher
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val productStore: WCProductStore,
    private val variationsCache: WooPosVariationsLRUCache,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover,
    @LimitedConcurrencyDispatcher private val searchDispatcher: CoroutineDispatcher,
) {
    private val searchByIdentifierContinuations =
        mutableMapOf<String, MutableList<ContinuationWrapper<Result<List<Product>>>>>()
    private val searchByGlobalUniqueIdContinuations =
        mutableMapOf<String, MutableList<ContinuationWrapper<Result<List<Product>>>>>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + searchDispatcher)

    init {
        dispatcher.register(this)
    }

    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat
    ): WooPosSearchByIdentifierResult = coroutineScope {
        val globalUniqueIdentifierSearchDeferred = async {
            searchAndConvertResult { searchProductsByGlobalUniqueId(identifier) }
        }

        val skuSearchDeferred = async {
            searchAndConvertResult { searchProductsBySku(identifier) }
        }

        val globalUniqueIdentifierResult = globalUniqueIdentifierSearchDeferred.await()

        if (globalUniqueIdentifierResult.isSuccess) {
            skuSearchDeferred.cancel()
            return@coroutineScope globalUniqueIdentifierResult
        }

        val identifierResult = skuSearchDeferred.await()

        if (identifierResult.isSuccess) {
            return@coroutineScope identifierResult
        }

        val identifierWithoutCheckDigit = checkDigitRemover(identifier, format)
        if (identifierWithoutCheckDigit != identifier) {
            val globalUniqueIdentifierFallbackDeferred = async {
                searchAndConvertResult { searchProductsByGlobalUniqueId(identifierWithoutCheckDigit) }
            }
            val identifierFallbackDeferred = async {
                searchAndConvertResult { searchProductsBySku(identifierWithoutCheckDigit) }
            }

            val globalUniqueIdentifierFallbackResult = globalUniqueIdentifierFallbackDeferred.await()
            if (globalUniqueIdentifierFallbackResult.isSuccess) {
                identifierFallbackDeferred.cancel()
                return@coroutineScope globalUniqueIdentifierFallbackResult
            }

            val identifierFallbackResult = identifierFallbackDeferred.await()
            if (identifierFallbackResult.isSuccess) {
                return@coroutineScope identifierFallbackResult
            }

            prioritizeError(globalUniqueIdentifierFallbackResult, identifierFallbackResult, globalUniqueIdentifierResult, identifierResult)
        } else {
            prioritizeError(globalUniqueIdentifierResult, identifierResult)
        }
    }

    private suspend fun searchProductsBySku(identifier: String): Result<List<Product>> {
        return performSearch(
            identifier = identifier,
            continuations = searchByIdentifierContinuations,
            createPayload = {
                WCProductStore.SearchProductsPayload(
                    site = selectedSite.get(),
                    searchQuery = identifier,
                    skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
                    pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                    offset = 0,
                    sorting = WCProductStore.ProductSorting.TITLE_ASC,
                    excludedProductIds = null,
                    filterOptions = emptyMap()
                )
            },
            dispatchAction = { payload ->
                dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
            }
        )
    }

    private suspend fun searchProductsByGlobalUniqueId(globalUniqueId: String): Result<List<Product>> {
        return performSearch(
            identifier = globalUniqueId,
            continuations = searchByGlobalUniqueIdContinuations,
            createPayload = {
                WCProductStore.SearchProductsByGlobalUniqueIdPayload(
                    site = selectedSite.get(),
                    globalUniqueId = globalUniqueId,
                    pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                    offset = 0,
                    sorting = WCProductStore.ProductSorting.TITLE_ASC,
                    excludedProductIds = null,
                    filterOptions = emptyMap()
                )
            },
            dispatchAction = { payload ->
                dispatcher.dispatch(WCProductActionBuilder.newSearchProductsByGlobalUniqueIdAction(payload))
            }
        )
    }

    private suspend fun <T> performSearch(
        identifier: String,
        continuations: MutableMap<String, MutableList<ContinuationWrapper<Result<List<Product>>>>>,
        createPayload: () -> T,
        dispatchAction: (T) -> Unit
    ): Result<List<Product>> {
        val continuation = ContinuationWrapper<Result<List<Product>>>(WooLog.T.PRODUCTS)

        return withContext(searchDispatcher) {
            val requestWithIdInProgress = continuations.containsKey(identifier)

            val continuationsList = continuations.getOrPut(identifier) { mutableListOf() }
            continuationsList.add(continuation)

            if (!requestWithIdInProgress) {
                val payload = createPayload()
                dispatchAction(payload)
            }

            val result = continuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {}

            when (result) {
                is ContinuationWrapper.ContinuationResult.Cancellation -> {
                    Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
                }

                is ContinuationWrapper.ContinuationResult.Success -> {
                    result.value
                }
            }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: WCProductStore.OnProductsSearched) {
        coroutineScope.launch {
            val query = event.globalUniqueIdSearchQuery ?: event.searchQuery ?: return@launch
            val continuations = when {
                event.globalUniqueIdSearchQuery != null -> searchByGlobalUniqueIdContinuations
                else -> searchByIdentifierContinuations
            }

            val continuationsList = continuations[query]

            val result = if (event.isError) {
                Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.NetworkError))
            } else {
                val productsList = event.searchResults.map { it.toAppModel() }
                Result.success(productsList)
            }

            continuationsList?.forEach { continuation ->
                continuation.continueWith(result)
            }

            continuations.remove(query)
        }
    }

    fun onCleanup() {
        dispatcher.unregister(this)

        searchByIdentifierContinuations.forEach { (_, continuationsList) ->
            continuationsList.forEach { it.cancel() }
        }
        searchByGlobalUniqueIdContinuations.forEach { (_, continuationsList) ->
            continuationsList.forEach { it.cancel() }
        }

        searchByIdentifierContinuations.clear()
        searchByGlobalUniqueIdContinuations.clear()
        coroutineScope.cancel()
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
