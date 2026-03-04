package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import javax.inject.Inject

class WooPosPaymentStatusResolver @Inject constructor(
    private val paymentStatusResolver: PaymentStatusResolver,
) {
    suspend fun resolve(orderId: Long): PaymentStatus {
        return paymentStatusResolver.resolve(orderId)
    }
}
