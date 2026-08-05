package com.woocommerce.android.ui.products

import com.woocommerce.android.model.BundleProductRules
import com.woocommerce.android.model.BundledProduct
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
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
                val products = getBundledProductsDetails(siteModel, remoteIds)

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
                        sku = product?.sku,
                        rules = BundleProductRules(
                            quantityMin = entity.quantityMin,
                            quantityMax = entity.quantityMax,
                            isOptional = entity.isOptional,
                            quantityDefault = entity.quantityDefault ?: 0f,
                            attributesDefault = entity.attributesDefault?.map { VariantOption(it) },
                            variationIds = entity.variationIds
                        ),
                        isVariable = product?.type == CoreProductType.VARIABLE.value
                    )
                }
            }
            .flowOn(dispatchers.io)
    }

    /**
     * The bundled products are not necessarily part of the cached product list, so any missing one is fetched to
     * make sure details such as the product type are available.
     *
     * The fetch returns a single page, so the ids are requested a page at a time. Asking for more than fits in one
     * would silently drop the remainder, leaving those products without a type.
     */
    private suspend fun getBundledProductsDetails(
        siteModel: SiteModel,
        remoteIds: List<Long>
    ): Map<Long, WCProductModel> {
        val cachedProducts = productStore.getProductsByRemoteIds(siteModel, remoteIds)
            .associateBy { it.remoteProductId }
        val missingIds = remoteIds - cachedProducts.keys
        if (missingIds.isEmpty()) return cachedProducts

        val fetchedProducts = missingIds.chunked(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE)
            .flatMap { idsToFetch -> productStore.fetchProductListSynced(siteModel, idsToFetch).orEmpty() }
            .associateBy { it.remoteProductId }
        return cachedProducts + fetchedProducts
    }
}
