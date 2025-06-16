package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import javax.inject.Inject

class WooPosSearchByIdentifierResultConverter @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationProcessor: WooPosSearchByIdentifierVariationProcessor
) {
    suspend operator fun invoke(
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
                    variationProcessor(product)
                } else {
                    productsCache.addAll(listOf(product))
                    WooPosSearchByIdentifierResult.Success(product)
                }
            }

            else -> handleError(result)
        }
    }

    private fun handleError(result: Result<List<Product>>): WooPosSearchByIdentifierResult {
        val error = result.exceptionOrNull()
        val searchError = when (error) {
            is WooPosSearchByIdentifierException -> error.error
            else -> WooPosSearchByIdentifierResult.Error.RequestCancelled
        }
        return WooPosSearchByIdentifierResult.Failure(searchError)
    }
}
