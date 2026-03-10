package com.woocommerce.android.ui.bookings

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
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
        val total = order.total.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return computeStatus(order.refundTotal.abs(), total, order.datePaid, order.status)
    }

    // Priority chain: refund checks first, then paid, then failed, then default unpaid.
    private fun computeStatus(
        refundTotal: BigDecimal,
        total: BigDecimal,
        datePaid: String,
        orderStatus: String,
    ): PaymentStatus = when {
        // Full refund: refund amount covers the entire order total
        refundTotal > BigDecimal.ZERO && total > BigDecimal.ZERO && refundTotal >= total -> PaymentStatus.REFUNDED
        // Partial refund: some refund issued but less than the order total
        refundTotal > BigDecimal.ZERO -> PaymentStatus.PARTIALLY_REFUNDED
        // Paid: the order has a recorded payment date
        datePaid.isNotEmpty() -> PaymentStatus.PAID
        // Failed: the linked order was either failed or cancelled
        orderStatus == "failed" || orderStatus == "cancelled" -> PaymentStatus.FAILED
        // Default: no payment recorded
        else -> PaymentStatus.UNPAID
    }
}
