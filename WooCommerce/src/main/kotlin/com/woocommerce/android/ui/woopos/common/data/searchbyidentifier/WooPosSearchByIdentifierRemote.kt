package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val skuSearch: WooPosSearchByIdentifierSkuSearch,
    private val gtinSearch: WooPosSearchByIdentifierGtinSearch,
    private val resultConverter: WooPosSearchByIdentifierResultConverter,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover,
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat
    ): WooPosSearchByIdentifierResult = coroutineScope {
        val gtinSearchDeferred = async {
            resultConverter { gtinSearch(identifier) }
        }

        val skuSearchDeferred = async {
            resultConverter { skuSearch(identifier) }
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
                resultConverter { gtinSearch(identifierWithoutCheckDigit) }
            }
            val identifierFallbackDeferred = async {
                resultConverter { skuSearch(identifierWithoutCheckDigit) }
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
}
