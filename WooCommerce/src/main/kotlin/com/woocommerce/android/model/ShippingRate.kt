package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class ShippingRate(
    val packageId: String,
    val rateId: String,
    val serviceId: String,
    val carrierId: String,
    val serviceName: String,
    val deliveryDays: Int,
    val price: BigDecimal,
    val formattedPrice: String,
    val option: Option
) : Parcelable {
    enum class Option {
        DEFAULT,
        SIGNATURE,
        ADULT_SIGNATURE
    }
}
