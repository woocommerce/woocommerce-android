package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuantityRules(
    val min: Int? = null,
    val max: Int? = null,
    val groupOf: Int? = null
) : Parcelable
