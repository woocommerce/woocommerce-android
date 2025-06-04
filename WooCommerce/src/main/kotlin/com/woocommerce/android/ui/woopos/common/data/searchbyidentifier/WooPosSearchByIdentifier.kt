package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote,
    private val productsCache: WooPosProductsCache,
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory
) {
    fun onCleanup() {
        remoteSearcher.onCleanup()
    }

    suspend operator fun invoke(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult {
        val localResult = searchLocally(codeScannerResultCode, codeScannerResultFormat)
        if (localResult.isSuccess) {
            return localResult
        }

        return searchRemotely(codeScannerResultCode, codeScannerResultFormat)
    }

    @Suppress("ReturnCount")
    private suspend fun searchLocally(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult {
        val searchQueries = listOfNotNull(
            codeScannerResultCode,
            removeCheckDigitIfPossible(codeScannerResultCode, codeScannerResultFormat)
        )

        for (query in searchQueries) {
            localSearcher.searchProductsBySku(query).firstOrNull()?.let { product ->
                return WooPosSearchByIdentifierResult.Success(product)
            }

            localSearcher.searchProductsByGlobalUniqueId(query).firstOrNull()?.let { product ->
                return WooPosSearchByIdentifierResult.Success(product)
            }
        }

        return WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)
    }

    private suspend fun searchRemotely(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult = coroutineScope {
        val globalUniqueIdentifierSearch = async {
            searchProductByGlobalUniqueIdentifierRemotely(
                codeScannerResultCode,
                codeScannerResultFormat
            )
        }
        val skuSearch = async {
            searchProductBySkuRemotely(
                codeScannerResultCode,
                codeScannerResultFormat
            )
        }

        val globalResult = globalUniqueIdentifierSearch.await()
        if (globalResult.isSuccess) {
            return@coroutineScope globalResult
        }

        val skuResult = skuSearch.await()
        if (skuResult.isSuccess) {
            return@coroutineScope skuResult
        }

        val globalError = globalResult.getErrorOrNull()
        val skuError = skuResult.getErrorOrNull()

        val prioritizedError = when {
            globalError == WooPosSearchByIdentifierResult.Error.RequestCancelled ||
                skuError == WooPosSearchByIdentifierResult.Error.RequestCancelled ->
                WooPosSearchByIdentifierResult.Error.RequestCancelled
            globalError == WooPosSearchByIdentifierResult.Error.NetworkError ||
                skuError == WooPosSearchByIdentifierResult.Error.NetworkError ->
                WooPosSearchByIdentifierResult.Error.NetworkError
            else -> WooPosSearchByIdentifierResult.Error.ProductNotFound
        }

        WooPosSearchByIdentifierResult.Failure(prioritizedError)
    }

    private suspend fun searchProductBySkuRemotely(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult {
        val remoteResult = remoteSearcher.searchProductsBySku(codeScannerResultCode)
        return handleRemoteSearchResult(
            remoteResult,
            codeScannerResultCode,
            codeScannerResultFormat,
            remoteSearcher::searchProductsBySku
        )
    }

    private suspend fun searchProductByGlobalUniqueIdentifierRemotely(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult {
        val remoteResult = remoteSearcher.searchProductsByGlobalUniqueId(codeScannerResultCode)
        return handleRemoteSearchResult(
            remoteResult,
            codeScannerResultCode,
            codeScannerResultFormat,
            remoteSearcher::searchProductsByGlobalUniqueId
        )
    }

    private fun removeCheckDigitIfPossible(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): String? {
        if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
            return checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat)
                .getSKUWithoutCheckDigit(codeScannerResultCode)
        }

        return null
    }

    @Suppress("ReturnCount", "NestedBlockDepth")
    private suspend fun handleRemoteSearchResult(
        remoteResult: Result<List<Product>>,
        fallbackCodeScannerResultCode: String,
        fallbackCodeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat,
        searchFunction: suspend (String) -> Result<List<Product>>
    ): WooPosSearchByIdentifierResult {
        if (remoteResult.isFailure) {
            val error = remoteResult.exceptionOrNull()
            val searchError = when (error) {
                is SearchException -> error.error
                else -> WooPosSearchByIdentifierResult.Error.RequestCancelled
            }
            return WooPosSearchByIdentifierResult.Failure(searchError)
        }

        val products = remoteResult.getOrThrow()
        val product = products.firstOrNull()
        if (product != null) {
            productsCache.addAll(listOf(product))
            return WooPosSearchByIdentifierResult.Success(product)
        }

        val fallbackIdentifier = removeCheckDigitIfPossible(
            fallbackCodeScannerResultCode,
            fallbackCodeScannerResultFormat
        )

        if (fallbackIdentifier != null) {
            val fallbackResult = searchFunction(fallbackIdentifier)
            if (fallbackResult.isSuccess) {
                val fallbackProducts = fallbackResult.getOrThrow()
                val fallbackProduct = fallbackProducts.firstOrNull()
                if (fallbackProduct != null) {
                    productsCache.addAll(listOf(fallbackProduct))
                    return WooPosSearchByIdentifierResult.Success(fallbackProduct)
                }
            } else {
                val error = fallbackResult.exceptionOrNull()
                val fallbackSearchError = when (error) {
                    is SearchException -> error.error
                    else -> WooPosSearchByIdentifierResult.Error.RequestCancelled
                }
                return WooPosSearchByIdentifierResult.Failure(fallbackSearchError)
            }
        }

        return WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)
    }

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isUPC() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isEAN() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13 ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
}
