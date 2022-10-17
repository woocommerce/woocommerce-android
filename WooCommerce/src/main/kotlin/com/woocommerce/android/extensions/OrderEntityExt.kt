package com.woocommerce.android.extensions

import org.wordpress.android.fluxc.model.OrderEntity

internal const val CASH_ON_DELIVERY_PAYMENT_TYPE = "cod"
internal const val WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE = "woocommerce_payments"
internal const val STRIPE_PAYMENTS_PAYMENT_TYPE = "stripe"
internal const val WOOCOMMERCE_BOOKINGS_PAYMENT_TYPE = "wc-booking-gateway"

val CASH_PAYMENTS = listOf(CASH_ON_DELIVERY_PAYMENT_TYPE, "bacs", "cheque")

val String.isCashPayment: Boolean
    get() = CASH_PAYMENTS.contains(this)

fun OrderEntity.getBillingName(defaultValue: String): String {
    return if (billingFirstName.isEmpty() && billingLastName.isEmpty()) {
        defaultValue
    } else "$billingFirstName $billingLastName"
}
