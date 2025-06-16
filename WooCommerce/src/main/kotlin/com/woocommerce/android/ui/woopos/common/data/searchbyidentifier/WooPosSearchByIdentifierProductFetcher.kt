package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierProductFetcher @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val productsCache: WooPosProductsCache
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(productId: Long): Product? {
        val cachedProduct = productsCache.getProductById(productId)
        if (cachedProduct != null) {
            return cachedProduct
        }

        val productResult = productStore.fetchSingleProduct(
            WCProductStore.FetchSingleProductPayload(
                site = selectedSite.get(),
                remoteProductId = productId,
            )
        )

        if (productResult.isError) {
            return null
        }

        return productStore.getProduct(selectedSite.get(), productId)
            ?.toAppModel()
            ?.also { product ->
                productsCache.addAll(listOf(product))
            }
    }
}