package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val skuSearch: WooPosSearchByIdentifierSkuSearch,
    private val globalUniqueIdSearch: WooPosSearchByIdentifierGlobalUniqueSearch,
    private val resultConverter: WooPosSearchByIdentifierResultConverter,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover,
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat
    ): WooPosSearchByIdentifierResult = coroutineScope {
        val globalUniqueIdentifierSearchDeferred = async {
            resultConverter { globalUniqueIdSearch(identifier) }
        }

        val skuSearchDeferred = async {
            resultConverter { skuSearch(identifier) }
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
                resultConverter { globalUniqueIdSearch(identifierWithoutCheckDigit) }
            }
            val identifierFallbackDeferred = async {
                resultConverter { skuSearch(identifierWithoutCheckDigit) }
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

            prioritizeError(
                globalUniqueIdentifierFallbackResult,
                identifierFallbackResult,
                globalUniqueIdentifierResult,
                identifierResult
            )
        } else {
            prioritizeError(globalUniqueIdentifierResult, identifierResult)
        }
    }

    private fun prioritizeError(
        vararg results: WooPosSearchByIdentifierResult
    ): WooPosSearchByIdentifierResult = results.filterIsInstance<WooPosSearchByIdentifierResult.Failure>().first()
}
