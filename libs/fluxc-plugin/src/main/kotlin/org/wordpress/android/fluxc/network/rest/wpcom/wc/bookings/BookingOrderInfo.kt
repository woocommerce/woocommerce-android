package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import androidx.room.Embedded

data class BookingOrderInfo(
    val status: String?,
    @Embedded(prefix = "product_") val productInfo: BookingProductInfo,
    @Embedded(prefix = "customer_") val customerInfo: BookingCustomerInfo
)

data class BookingProductInfo(
    val name: String?
)

data class BookingCustomerInfo(
    val billingFirstName: String?,
    val billingLastName: String?,
    val billingCompany: String?,
    val billingAddress1: String?,
    val billingAddress2: String?,
    val billingCity: String?,
    val billingState: String?,
    val billingPostcode: String?,
    val billingCountry: String?,
    val billingEmail: String?,
    val billingPhone: String?
)
