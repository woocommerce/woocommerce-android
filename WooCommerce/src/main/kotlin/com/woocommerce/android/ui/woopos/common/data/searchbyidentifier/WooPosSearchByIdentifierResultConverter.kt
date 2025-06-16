package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import javax.inject.Inject

class WooPosSearchByIdentifierResultConverter @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationProcess: WooPosSearchByIdentifierVariationProcess
) {
    suspend operator fun invoke(
        searchFunction: suspend () -> WooPosSearchByIdentifierResult
    ): WooPosSearchByIdentifierResult {
        val result = searchFunction()

        return when (result) {
            is WooPosSearchByIdentifierResult.Success -> {
                val product = result.product
                if (product.type.equals("variation", ignoreCase = true)) {
                    variationProcess(product)
                } else {
                    productsCache.addAll(listOf(product))
                    result
                }
            }

            is WooPosSearchByIdentifierResult.VariationSuccess -> {
                productsCache.addAll(listOf(result.parentProduct))
                result
            }

            is WooPosSearchByIdentifierResult.Failure -> result
        }
    }
}
