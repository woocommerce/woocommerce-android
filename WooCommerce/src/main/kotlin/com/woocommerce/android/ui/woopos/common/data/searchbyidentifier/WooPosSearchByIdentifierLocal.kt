package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val lastDigitRemover: WooPosSearchByIdentifierLastDigitRemover
) {
    @Suppress("ReturnCount")
    suspend fun searchProduct(
        identifier: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Product? {
        val searchQueries = listOfNotNull(
            identifier,
            lastDigitRemover(identifier, codeScannerResultFormat)
        )

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
