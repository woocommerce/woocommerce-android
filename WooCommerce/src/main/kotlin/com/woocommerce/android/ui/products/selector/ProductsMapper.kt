package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class ProductsMapper @Inject constructor(
    private val site: SelectedSite,
    private val productStore: WCProductStore
) {
    suspend fun mapProductIdsToProduct(productIds: List<Long>): List<Product> {
        return productIds.asProductList(site.get()).map { product ->
            product.toAppModel()
        }
    }

    /**
     * This method gets all Products from the IDs described by the
     * List<Long>, but it only gets the product that are already available in the database.
     */
    private suspend fun List<Long>.asProductList(
        site: SiteModel,
    ): List<WCProductModel> = filter { productStore.getProductExistsByRemoteId(site, it) }
        .mapNotNull { productStore.getProduct(site, it) }
}
