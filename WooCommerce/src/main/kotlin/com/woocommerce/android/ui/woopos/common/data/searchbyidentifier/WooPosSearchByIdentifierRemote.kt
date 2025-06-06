package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierRemote @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
) {
    private val searchByIdentifierContinuations =
        ConcurrentHashMap<String, MutableList<ContinuationWrapper<List<Product>>>>()
    private val searchByGlobalUniqueIdContinuations =
        ConcurrentHashMap<String, MutableList<ContinuationWrapper<List<Product>>>>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + searchDispatcher)

    init {
        dispatcher.register(this)
    }

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
        if (gtinResult is WooPosSearchByIdentifierResult.Success) {
            skuSearchDeferred.cancel()
            return@coroutineScope gtinResult
        }

        val identifierResult = skuSearchDeferred.await()
        if (identifierResult is WooPosSearchByIdentifierResult.Success) {
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
            if (gtinFallbackResult is WooPosSearchByIdentifierResult.Success) {
                identifierFallbackDeferred.cancel()
                return@coroutineScope gtinFallbackResult
            }

            val identifierFallbackResult = identifierFallbackDeferred.await()
            if (identifierFallbackResult is WooPosSearchByIdentifierResult.Success) {
                return@coroutineScope identifierFallbackResult
            }

            prioritizeError(gtinFallbackResult, identifierFallbackResult, gtinResult, identifierResult)
        } else {
            prioritizeError(gtinResult, identifierResult)
        }
    }

    private suspend fun searchProductsBySku(identifier: String): Result<List<Product>> {
        return performSearch(
            identifier = identifier,
            continuations = searchByIdentifierContinuations,
            createPayload = { query ->
                WCProductStore.SearchProductsPayload(
                    site = selectedSite.get(),
                    searchQuery = query,
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
            createPayload = { query ->
                WCProductStore.SearchProductsByGlobalUniqueIdPayload(
                    site = selectedSite.get(),
                    globalUniqueId = query,
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
        continuations: ConcurrentHashMap<String, MutableList<ContinuationWrapper<List<Product>>>>,
        createPayload: (String) -> T,
        dispatchAction: (T) -> Unit
    ): Result<List<Product>> {
        val continuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)

        return withContext(searchDispatcher) {
            val requestWithIdInProgress = continuations.containsKey(identifier)

            val continuationsList = continuations.getOrPut(identifier) { mutableListOf() }
            continuationsList.add(continuation)

            if (!requestWithIdInProgress) {
                val payload = createPayload(identifier)
                dispatchAction(payload)
            }

            val result = continuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {}

            when (result) {
                is ContinuationWrapper.ContinuationResult.Cancellation ->
                    Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))

                is ContinuationWrapper.ContinuationResult.Success ->
                    Result.success(result.value)
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
                if (product != null) {
                    productsCache.addAll(listOf(product))
                    WooPosSearchByIdentifierResult.Success(product)
                } else {
                    WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)
                }
            }

            else -> handleError(result)
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

            val products = if (event.isError) {
                emptyList()
            } else {
                val productsList = event.searchResults.map { it.toAppModel() }
                productsList
            }

            continuationsList?.forEach { continuation ->
                continuation.continueWith(products)
            }

            continuations.remove(query)
        }
    }

    fun onCleanup() {
        dispatcher.unregister(this)

        // Cancel any pending continuations
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
}

internal class SearchException(val error: WooPosSearchByIdentifierResult.Error) : Exception()
