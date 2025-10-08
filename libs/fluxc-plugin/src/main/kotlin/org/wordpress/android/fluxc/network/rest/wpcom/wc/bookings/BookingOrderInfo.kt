package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class BookingOrderInfo(
    val status: String? = null,
    @Embedded(prefix = "product_") val productInfo: BookingProductInfo = BookingProductInfo(),
    @Embedded(prefix = "customer_") val customerInfo: BookingCustomerInfo = BookingCustomerInfo(),
)

data class BookingProductInfo(
    @ColumnInfo(defaultValue = "") val name: String = "",
)

data class BookingCustomerInfo(
    @ColumnInfo(defaultValue = "") val billingFirstName: String = "",
    @ColumnInfo(defaultValue = "") val billingLastName: String = "",
    @ColumnInfo(defaultValue = "") val billingCompany: String = "",
    @ColumnInfo(defaultValue = "") val billingAddress1: String = "",
    @ColumnInfo(defaultValue = "") val billingAddress2: String = "",
    @ColumnInfo(defaultValue = "") val billingCity: String = "",
    @ColumnInfo(defaultValue = "") val billingState: String = "",
    @ColumnInfo(defaultValue = "") val billingPostcode: String = "",
    @ColumnInfo(defaultValue = "") val billingCountry: String = "",
    @ColumnInfo(defaultValue = "") val billingEmail: String = "",
    @ColumnInfo(defaultValue = "") val billingPhone: String = ""
)
