package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
import javax.inject.Inject

class WooPosPaymentStatusResolver @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
) {
    suspend fun resolve(orderId: Long, orderTotal: BigDecimal?): PaymentStatus {
        val order = orderStore.getOrderByIdAndSite(orderId, selectedSite.get())
            ?: return PaymentStatus.UNPAID

        return computeStatus(order.refundTotal.abs(), orderTotal ?: BigDecimal.ZERO, order.datePaid, order.status)
    }

    private fun computeStatus(
        refundTotal: BigDecimal,
        total: BigDecimal,
        datePaid: String,
        orderStatus: String,
    ): PaymentStatus = when {
        refundTotal > BigDecimal.ZERO && total > BigDecimal.ZERO && refundTotal >= total -> PaymentStatus.REFUNDED
        refundTotal > BigDecimal.ZERO -> PaymentStatus.PARTIALLY_REFUNDED
        datePaid.isNotEmpty() -> PaymentStatus.PAID
        orderStatus == "failed" || orderStatus == "cancelled" -> PaymentStatus.FAILED
        else -> PaymentStatus.UNPAID
    }
}
