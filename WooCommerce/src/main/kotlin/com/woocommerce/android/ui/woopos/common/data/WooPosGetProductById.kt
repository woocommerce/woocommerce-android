package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val selectedSite: SelectedSite,
    private val cache: WooPosProductsCache,
    private val productRestClient: ProductRestClient,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        val cachedProduct = cache.getProductById(productId)
        if (cachedProduct != null) {
            return@withContext cachedProduct
        }

        if (cache.getAll().size >= WooPosProductsCache.MAX_CACHE_SIZE) {
            val remoteProductResult = productRestClient.fetchSingleProduct(
                site = selectedSite.get(),
                remoteProductId = productId,
            )

            return@withContext if (!remoteProductResult.isError) {
                val remoteProduct = remoteProductResult.productWithMetaData.product
                val product = remoteProduct.toAppModel()
                cache.addAll(listOf(product))
                product
            } else {
                null
            }
        } else {
            null
        }
    }
}
