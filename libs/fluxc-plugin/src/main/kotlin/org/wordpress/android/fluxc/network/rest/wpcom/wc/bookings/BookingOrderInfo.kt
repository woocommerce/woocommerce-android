package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class BookingOrderInfo(
    val status: String? = null,
    @Embedded(prefix = "product_") val productInfo: BookingProductInfo? = null,
    @Embedded(prefix = "customer_") val customerInfo: BookingCustomerInfo? = null,
)

data class BookingProductInfo(
    @ColumnInfo val name: String,
)

data class BookingCustomerInfo(
    @ColumnInfo val billingFirstName: String? = null,
    @ColumnInfo val billingLastName: String? = null,
    @ColumnInfo val billingCompany: String? = null,
    @ColumnInfo val billingAddress1: String? = null,
    @ColumnInfo val billingAddress2: String? = null,
    @ColumnInfo val billingCity: String? = null,
    @ColumnInfo val billingState: String? = null,
    @ColumnInfo val billingPostcode: String? = null,
    @ColumnInfo val billingCountry: String? = null,
    @ColumnInfo val billingEmail: String? = null,
    @ColumnInfo val billingPhone: String? = null,
)
