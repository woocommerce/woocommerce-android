package com.woocommerce.android.ui.bookings

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class PaymentStatusResolver @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
) {
    suspend fun resolve(orderId: Long): PaymentStatus {
        val order = orderStore.getOrderByIdAndSite(orderId, selectedSite.get())
            ?: return PaymentStatus.UNPAID

        return statusForOrder(order)
    }

    suspend fun resolveAll(orderIds: List<Long>): Map<Long, PaymentStatus> {
        val uniqueOrderIds = orderIds.distinct()
        if (uniqueOrderIds.isEmpty()) return emptyMap()

        val ordersById = orderStore.getOrdersByIdsAndSite(uniqueOrderIds, selectedSite.get())
            .associateBy { it.orderId }

        return uniqueOrderIds.associateWith { orderId ->
            ordersById[orderId]?.let(::statusForOrder) ?: PaymentStatus.UNPAID
        }
    }

    private fun statusForOrder(order: OrderEntity): PaymentStatus {
        val apiValue = order.metaData[WCMetaData.PaymentMetadataKeys.PAYMENT_STATUS]?.valueAsString
        return PaymentStatus.fromApiValue(apiValue ?: "")
    }
}
