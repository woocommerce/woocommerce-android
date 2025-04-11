package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val cache: WooPosProductsCache,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        cache.getProductById(productId)
    }
}
