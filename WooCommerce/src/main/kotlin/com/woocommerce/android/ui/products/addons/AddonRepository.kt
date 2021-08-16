package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class AddonRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    fun fetchOrderAddonsData(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = getOrder(orderID)
        ?.findOrderAttributesWith(orderItemID)
        ?.joinWithAddonsFrom(productID)

    private fun getOrder(orderID: Long) =
        orderStore.getOrderByIdentifier(
            OrderIdentifier(selectedSite.get().id, orderID)
        )

    private fun WCOrderModel.findOrderAttributesWith(orderItemID: Long) =
        getLineItemList().find { it.id == orderItemID }
            ?.getAttributeList()
            ?.map { Attribute(it.key.orEmpty(), it.value.orEmpty()) }
            ?.filter { it.isNotInternalAttributeData }

    private fun List<Attribute>.joinWithAddonsFrom(productID: Long) =
        productStore
            .getProductByRemoteId(selectedSite.get(), productID)
            ?.addons?.map { it.toAppModel() }
            ?.let { addons -> Pair(addons, this) }
}
