package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import java.math.BigDecimal
import javax.inject.Inject

class WooPosPaymentStatusResolver @Inject constructor(
    private val paymentStatusResolver: PaymentStatusResolver,
) {
    suspend fun resolve(orderId: Long, orderTotal: BigDecimal?): PaymentStatus {
        return paymentStatusResolver.resolve(orderId, orderTotal)
    }
}
