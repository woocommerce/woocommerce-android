package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(
        identifier: String,
        codeScannerResultFormat: WooPosBarcodeFormat
    ): Product? {
        val identifierWithoutCheckDigit = checkDigitRemover(identifier, codeScannerResultFormat)
        val searchQueries = if (identifierWithoutCheckDigit != identifier) {
            listOf(identifier, identifierWithoutCheckDigit)
        } else {
            listOf(identifier)
        }

        val allProducts = productsCache.getAll()

        for (query in searchQueries) {
            allProducts.firstOrNull { product ->
                product.globalUniqueId.equals(query, ignoreCase = true)
            }?.let { return it }

            allProducts.firstOrNull { product ->
                product.sku.equals(query, ignoreCase = true)
            }?.let { return it }
        }

        return null
    }
}
