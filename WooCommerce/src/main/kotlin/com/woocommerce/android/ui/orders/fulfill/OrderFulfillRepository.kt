package com.woocommerce.android.ui.orders.fulfill

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

@OpenClassOnDebug
class OrderFulfillRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore,
    private val productStore: WCProductStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite
) {
    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)?.toAppModel()

    fun getNonRefundedProducts(
        remoteOrderId: Long,
        items: List<Order.Item>
    ) = getOrderRefunds(remoteOrderId).getNonRefundedProducts(items)

    fun hasVirtualProductsOnly(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getVirtualProductCountByRemoteIds(
                selectedSite.get(), remoteProductIds
            ) == remoteProductIds.size
        } else false
    }

    fun hasShippingLabels(remoteOrderId: Long) = shippingLabelStore
        .getShippingLabelsForOrder(selectedSite.get(), remoteOrderId).isNotEmpty()

    fun getOrderShipmentTrackings(localOrderId: Int) =
        orderStore.getShipmentTrackingsForOrder(selectedSite.get(), localOrderId).map { it.toAppModel() }

    private fun getOrderRefunds(remoteOrderId: Long) = refundStore
        .getAllRefunds(selectedSite.get(), remoteOrderId)
        .map { it.toAppModel() }
        .reversed()
        .sortedBy { it.id }
}
