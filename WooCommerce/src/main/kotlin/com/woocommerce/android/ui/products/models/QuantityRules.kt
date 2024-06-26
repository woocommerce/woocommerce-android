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
        get() = min?.let { it > 0 } ?: false || max?.let { it > 0 } ?: false || groupOf?.let { it > 0} ?: false
    val allRulesAreNull: Boolean
        get() = min == null && max == null && groupOf == null
}
