package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item.Attribute
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
    ) = order.findAttributesWith(productID)
        ?.joinWithAddonsFrom(productID)

    private fun Order.findAttributesWith(productID: Long) =
        items.find { it.productId == productID }
            ?.attributesList

    private fun List<Attribute>.joinWithAddonsFrom(productID: Long) =
        productStore
            .getProductByRemoteId(selectedSite.get(), productID)
            ?.toAppModel()
            ?.addons
            ?.let { addons -> Pair(addons, this) }
}
