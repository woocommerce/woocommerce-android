package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosSearchByIdentifierVariationProcess @Inject constructor(
    private val variationGetOrFetcher: WooPosSearchByIdentifierVariationGetOrFetch,
    private val productFetch: WooPosSearchByIdentifierProductFetch
) {
    suspend operator fun invoke(product: Product): WooPosSearchByIdentifierResult = coroutineScope {
        val parentId = product.parentId
        val variationId = product.remoteId

        if (parentId <= 0) {
            return@coroutineScope WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ProductNotFound
            )
        }

        val variationJob = async { variationGetOrFetcher(variationId, parentId) }
        val parentProductJob = async { productFetch(parentId) }

        val variationResult = variationJob.await()
        val parentProductResult = parentProductJob.await()

        return@coroutineScope when {
            variationResult is WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.Success &&
                parentProductResult is WooPosSearchByIdentifierResult.Success -> {
                WooPosSearchByIdentifierResult.VariationSuccess(
                    variationResult.variation,
                    parentProductResult.product
                )
            }

            else -> listOf(
                variationResult,
                parentProductResult
            ).filterIsInstance<WooPosSearchByIdentifierResult.Failure>()
                .first()
        }
    }
}
