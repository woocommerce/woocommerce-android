package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShippingMethod(
    val id: String,
    val title: String
) : Parcelable
