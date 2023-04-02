package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuantityRules(
    val min: Int?,
    val max: Int?,
    val groupOf: Int?
) : Parcelable
