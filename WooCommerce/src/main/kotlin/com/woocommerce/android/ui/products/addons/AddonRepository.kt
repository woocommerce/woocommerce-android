package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@OpenClassOnDebug
class AddonRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    fun fetchOrderAddonsData(
        orderID: Long,
        productID: Long
    ) = fetchOrder(orderID)
        ?.findOrderAttributesWith(productID)
        ?.joinWithAddonsFrom(productID)

    private fun fetchOrder(orderID: Long) =
        orderStore.getOrderByIdentifier(
            OrderIdentifier(selectedSite.get().id, orderID)
        )

    private fun WCOrderModel.findOrderAttributesWith(productID: Long) =
        getLineItemList().find { it.productId == productID }
            ?.getAttributeList()
            ?.mapNotNull { it.key }

    private fun List<String>.joinWithAddonsFrom(productID: Long) =
        productStore
            .getProductByRemoteId(selectedSite.get(), productID)
            ?.toAppModel()
            ?.addons
            ?.let { addons -> Pair(addons, this) }
}
