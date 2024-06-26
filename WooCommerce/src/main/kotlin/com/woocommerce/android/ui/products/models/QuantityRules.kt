package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuantityRules(
    val min: Int? = null,
    val max: Int? = null,
    val groupOf: Int? = null
) : Parcelable {
    val hasAtLeastOneValidRule: Boolean
        get() = (min ?: 0) > 0 || (max ?: 0) > 0 || (groupOf ?: 0) > 0
    val allRulesAreNull: Boolean
        get() = min == null && max == null && groupOf == null
}
