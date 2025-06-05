package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover
) {
    private var searchByIdentifierContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)
    private var searchByGlobalUniqueIdContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)

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
        if (identifierWithoutCheckDigit != null) {
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
        val result = searchByIdentifierContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsPayload(
                site = selectedSite.get(),
                searchQuery = identifier,
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation ->
                Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))

            is ContinuationWrapper.ContinuationResult.Success -> Result.success(result.value)
        }
    }

    private suspend fun searchProductsByGlobalUniqueId(globalUniqueId: String): Result<List<Product>> {
        val result = searchByGlobalUniqueIdContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsByGlobalUniqueIdPayload(
                site = selectedSite.get(),
                globalUniqueId = globalUniqueId,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsByGlobalUniqueIdAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation ->
                Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))

            is ContinuationWrapper.ContinuationResult.Success ->
                Result.success(result.value)
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: WCProductStore.OnProductsSearched) {
        val continuation = if (event.globalUniqueIdSearchQuery != null) {
            searchByGlobalUniqueIdContinuation
        } else {
            searchByIdentifierContinuation
        }

        if (event.isError) {
            continuation.continueWith(emptyList())
        } else {
            val products = event.searchResults.map { it.toAppModel() }
            continuation.continueWith(products)
        }
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }
}

internal class SearchException(val error: WooPosSearchByIdentifierResult.Error) : Exception()
