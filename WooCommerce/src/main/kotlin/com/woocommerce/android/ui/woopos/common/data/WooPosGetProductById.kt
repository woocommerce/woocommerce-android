package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val selectedSite: SelectedSite,
    private val cache: WooPosProductsCache,
    private val productRestClient: ProductRestClient,
    private val wcProductToWooPosProductModelMapper: WooPosWCProductToWooPosProductModelMapper,
    private val productMapper: WooPosProductModelMapper,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
) {
    suspend operator fun invoke(productId: Long): WooPosProductModel? = withContext(IO) {
        when {
            wooPosLocalCatalogM1Enabled() -> {
                val result = localCatalogStore.getProduct(
                    siteId = LocalOrRemoteId.LocalId(selectedSite.get().id),
                    remoteProductId = LocalOrRemoteId.RemoteId(productId)
                )
                result.getOrNull()?.let { entity ->
                    productMapper.fromEntity(entity)
                }
            }

            else -> getFromInMemoryCache(productId)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun getFromInMemoryCache(productId: Long): WooPosProductModel? {
        val cachedProduct = cache.getProductById(productId)
        if (cachedProduct != null) {
            return cachedProduct
        }

        return if (cache.getAll().size >= WooPosProductsCache.MAX_CACHE_SIZE) {
            val remoteProductResult = productRestClient.fetchSingleProduct(
                site = selectedSite.get(),
                remoteProductId = productId,
            )

            return if (!remoteProductResult.isError) {
                val remoteProduct = remoteProductResult.productWithMetaData.product
                val product = wcProductToWooPosProductModelMapper.map(remoteProduct)
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
