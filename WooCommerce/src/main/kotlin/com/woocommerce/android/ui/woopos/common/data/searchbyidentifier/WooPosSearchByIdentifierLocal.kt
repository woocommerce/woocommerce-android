package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationsCache: WooPosVariationsLRUCache,
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(
        identifier: String,
        codeScannerResultFormat: WooPosBarcodeFormat
    ): WooPosSearchByIdentifierResult? {
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
            }?.let { return WooPosSearchByIdentifierResult.Success(it) }

            allProducts.firstOrNull { product ->
                product.sku.equals(query, ignoreCase = true)
            }?.let { return WooPosSearchByIdentifierResult.Success(it) }
        }

        val allVariations = variationsCache.getAll()
        for (query in searchQueries) {
            allVariations.firstOrNull { variation ->
                variation.globalUniqueId.equals(query, ignoreCase = true)
            }?.let {
                val parentProduct = productsCache.getProductById(it.remoteProductId)
                return WooPosSearchByIdentifierResult.VariationSuccess(it, parentProduct!!)
            }

            allVariations.firstOrNull { variation ->
                variation.sku.equals(query, ignoreCase = true)
            }?.let {
                val parentProduct = productsCache.getProductById(it.remoteProductId)
                return WooPosSearchByIdentifierResult.VariationSuccess(it, parentProduct!!)
            }
        }

        return null
    }
}
