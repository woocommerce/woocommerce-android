package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val selectedSite: SelectedSite,
    private val cache: WooPosProductsCache,
    private val productsStore: WCProductStore,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        val cachedProduct = cache.getProductById(productId)
        cachedProduct
            ?: if (cache.getAll().size >= WooPosProductsCache.MAX_CACHE_SIZE) {
                cache.getProductById(productId) ?: productsStore.getProductByRemoteId(
                    site = selectedSite.get(),
                    remoteProductId = productId,
                )?.toAppModel()?.let {
                    cache.addAll(listOf(it))
                    it
                }
            } else {
                null
            }
    }
}
