package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SiteParameters(
    val currencyCode: String?,
    val weightUnit: String?,
    val dimensionUnit: String?,
    val gmtOffset: Float
) : Parcelable
