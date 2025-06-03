package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosGetProductByGtinOrSku @Inject constructor(
    private val cache: WooPosProductsCache,
) {
    suspend operator fun invoke(gtin: String): Product = withContext(IO) {
        val cachedProduct = cache.getProductByGtin(gtin)
        if (cachedProduct != null) {
            return@withContext cachedProduct
        }
        throw NotImplementedError("Fetching product by GTIN is not implemented yet.")
    }
}

