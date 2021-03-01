package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShippingRate(
    val rateId: String,
    val serviceId: String,
    val carrierId: Int,
    val serviceName: String
) : Parcelable
