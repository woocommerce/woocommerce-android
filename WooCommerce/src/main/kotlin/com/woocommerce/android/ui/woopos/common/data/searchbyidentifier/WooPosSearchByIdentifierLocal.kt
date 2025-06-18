package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationsCache: WooPosVariationsLRUCache,
) {
    suspend operator fun invoke(identifier: String): WooPosSearchByIdentifierResult {
        val allProducts = productsCache.getAll()

        allProducts.firstOrNull { product ->
            product.globalUniqueId.equals(identifier, ignoreCase = true)
        }?.let { return WooPosSearchByIdentifierResult.Success(it) }

        val allVariations = variationsCache.getAll()
        return allVariations.firstOrNull { variation ->
            variation.globalUniqueId.equals(identifier, ignoreCase = true)
        }?.let {
            val parentProduct = productsCache.getProductById(it.remoteProductId)
            WooPosSearchByIdentifierResult.VariationSuccess(it, parentProduct!!)
        } ?: WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NotFound
        )
    }
}
