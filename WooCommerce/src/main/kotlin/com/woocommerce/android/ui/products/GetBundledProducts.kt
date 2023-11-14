package com.woocommerce.android.ui.products

import com.woocommerce.android.model.BundledProduct
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GetBundledProducts @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    operator fun invoke(productId: Long): Flow<List<BundledProduct>> {
        val siteModel = selectedSite.get()
        return productStore.observeBundledProducts(siteModel, productId)
            .map { list ->
                val remoteIds = list.map { it.bundledProductId }.distinct()
                val products =
                    productStore.getProductsByRemoteIds(siteModel, remoteIds).associateBy { it.remoteProductId }

                list.map { entity ->
                    val product = products[entity.bundledProductId]
                    val image = product?.getFirstImageUrl()
                    BundledProduct(
                        id = entity.id,
                        parentProductId = productId,
                        bundledProductId = entity.bundledProductId,
                        title = entity.title,
                        stockStatus = ProductStockStatus.fromString(entity.stockStatus.replace("_", "")),
                        imageUrl = image,
                        sku = product?.sku
                    )
                }
            }
            .flowOn(dispatchers.io)
    }
}
