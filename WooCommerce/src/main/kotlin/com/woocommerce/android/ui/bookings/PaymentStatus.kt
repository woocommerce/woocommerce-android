package com.woocommerce.android.ui.bookings

enum class PaymentStatus {
    PAID,
    UNPAID,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    AUTHORIZED,
    AUTHORIZATION_VOIDED;

    companion object {
        fun fromApiValue(value: String): PaymentStatus = when (value) {
            "paid" -> PAID
            "unpaid" -> UNPAID
            "failed" -> FAILED
            "refunded" -> REFUNDED
            "partially_refunded" -> PARTIALLY_REFUNDED
            "authorized" -> AUTHORIZED
            "authorization_voided" -> AUTHORIZATION_VOIDED
            else -> UNPAID
        }
    }
}
