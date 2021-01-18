package com.woocommerce.android.ui.products

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductFilterResult(
    val stockStatus: String?,
    val productType: String?,
    val productStatus: String?
) : Parcelable
