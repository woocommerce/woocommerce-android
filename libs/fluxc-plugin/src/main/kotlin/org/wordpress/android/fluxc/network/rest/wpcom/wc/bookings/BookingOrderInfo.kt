package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import androidx.room.Embedded
import java.math.BigDecimal

data class BookingOrderInfo(
    val status: String? = null,
    @Embedded(prefix = "product_") val productInfo: BookingProductInfo? = null,
    @Embedded(prefix = "customer_") val customerInfo: BookingCustomerInfo? = null,
    @Embedded(prefix = "payment_") val paymentInfo: BookingPaymentInfo? = null,
)

data class BookingProductInfo(
    val name: String,
)

data class BookingCustomerInfo(
    val billingFirstName: String? = null,
    val billingLastName: String? = null,
    val billingCompany: String? = null,
    val billingAddress1: String? = null,
    val billingAddress2: String? = null,
    val billingCity: String? = null,
    val billingState: String? = null,
    val billingPostcode: String? = null,
    val billingCountry: String? = null,
    val billingEmail: String? = null,
    val billingPhone: String? = null,
)

data class BookingPaymentInfo(
    val paymentMethodId: String?,
    val paymentMethodTitle: String?,
    val subtotal: BigDecimal,
    val subtotalTax: BigDecimal,
    val total: BigDecimal,
    val totalTax: BigDecimal,
)
