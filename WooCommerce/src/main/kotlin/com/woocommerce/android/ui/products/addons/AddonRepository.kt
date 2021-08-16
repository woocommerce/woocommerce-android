package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item.Attribute
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
    fun orderItemContainsAddons(
        item: Order.Item
    ) = productStore.getProductByRemoteId(selectedSite.get(), item.productId)
        ?.addons?.isNotEmpty()
        ?: false

    fun fetchOrderAddonsData(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = fetchOrder(orderID)
        ?.findOrderAttributesWith(orderItemID)
        ?.joinWithAddonsFrom(productID)

    private fun fetchOrder(orderID: Long) =
        orderStore.getOrderByIdentifier(
            OrderIdentifier(selectedSite.get().id, orderID)
        )

    private fun WCOrderModel.findOrderAttributesWith(orderItemID: Long) =
        getLineItemList().find { it.id == orderItemID }
            ?.getAttributeList()
            ?.filter { it.key?.first().toString() != "_" }
            ?.map { Attribute(it.key.orEmpty(), it.value.orEmpty()) }

    private fun List<Attribute>.joinWithAddonsFrom(productID: Long) =
        productStore
            .getProductByRemoteId(selectedSite.get(), productID)
            ?.addons?.map { it.toAppModel() }
            ?.let { addons -> Pair(addons, this) }
}
