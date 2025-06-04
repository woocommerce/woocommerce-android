package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache
) {
    suspend fun searchProductsBySku(sku: String): List<Product> {
        return productsCache.getAll().filter { product ->
            product.sku.equals(sku, ignoreCase = true)
        }
    }

    suspend fun searchProductsByGlobalUniqueId(globalUniqueId: String): List<Product> {
        return productsCache.getAll().filter { product ->
            product.globalUniqueId.equals(globalUniqueId, ignoreCase = true)
        }
    }
}
