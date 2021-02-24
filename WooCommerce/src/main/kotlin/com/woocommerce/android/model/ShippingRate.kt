package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class ShippingRate(
    val id: String,
    val title: String,
    val deliveryEstimate: Int,
    val price: BigDecimal,
    val carrier: ShippingCarrier
) : Parcelable {
    enum class ShippingCarrier {
        FEDEX,
        USPS,
        UPS
    }
}
