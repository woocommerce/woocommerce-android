package com.woocommerce.android.ui.products

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductFilterResult(
    val stockStatus: String?,
    val productType: String?,
    val productStatus: String?
) : Parcelable
