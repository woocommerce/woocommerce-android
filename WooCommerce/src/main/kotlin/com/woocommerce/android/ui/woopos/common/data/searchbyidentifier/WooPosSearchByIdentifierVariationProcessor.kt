package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosSearchByIdentifierVariationProcessor @Inject constructor(
    private val variationFetcher: WooPosSearchByIdentifierVariationFetcher,
    private val productFetcher: WooPosSearchByIdentifierProductFetcher
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(product: Product): WooPosSearchByIdentifierResult = coroutineScope {
        val parentId = product.parentId
        val variationId = product.remoteId

        if (parentId <= 0) {
            return@coroutineScope WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ProductNotFound
            )
        }

        val variationJob = async { variationFetcher(variationId, parentId) }
        val parentProductJob = async { productFetcher(parentId) }

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
}