package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@OpenClassOnDebug
class AddonRepository @Inject constructor(
    private val addonStore: WCAddonsStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    fun fetchOrderedAddonsData(
        order: Order,
        productID: Long
    ): Pair<List<ProductAddon>, List<Order.Item.Attribute>>? =
        order.findAttributesFromProduct(productID)
            ?.let { attributes ->
                productStore
                    .getProductByRemoteId(selectedSite.get(), productID)
                    ?.toAppModel()
                    ?.addons
                    ?.let { addons -> Pair(addons, attributes) }
            }

    private fun Order.findAttributesFromProduct(productID: Long) =
        items.find { it.productId == productID }
            ?.attributesList
}
