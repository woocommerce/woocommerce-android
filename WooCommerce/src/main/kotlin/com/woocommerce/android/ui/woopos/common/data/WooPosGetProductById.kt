package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val selectedSite: SelectedSite,
    private val cache: WooPosProductsCache,
    private val productRestClient: ProductRestClient,
    private val productMapper: WooPosWCProductToWooPosProductModelMapper,
) {
    suspend operator fun invoke(productId: Long): WooPosProductModel? = withContext(IO) {
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
                val product = productMapper.map(remoteProduct)
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
