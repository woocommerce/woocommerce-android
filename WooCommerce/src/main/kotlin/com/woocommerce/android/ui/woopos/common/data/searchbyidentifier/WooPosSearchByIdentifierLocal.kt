package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationsCache: WooPosVariationsLRUCache,
) {
    suspend operator fun invoke(identifier: String): WooPosSearchByIdentifierResult {
        findProductByIdentifier(identifier)?.let {
            return WooPosSearchByIdentifierResult.Success(it)
        }

        return findVariationWithParentByIdentifier(identifier) ?: WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NotFound
        )
    }

    private suspend fun findProductByIdentifier(identifier: String) =
        productsCache.getAll().firstOrNull { product ->
            product.globalUniqueId.equals(identifier, ignoreCase = true)
        }

    private suspend fun findVariationWithParentByIdentifier(identifier: String):
        WooPosSearchByIdentifierResult.VariationSuccess? {
        val variation = variationsCache.getAll().firstOrNull { variation ->
            variation.globalUniqueId.equals(identifier, ignoreCase = true)
        } ?: return null

        return productsCache.getProductById(variation.remoteProductId)?.let { parentProduct ->
            WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct)
        }
    }
}
