package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.math.BigDecimal

/**
 * Represent states of the [Collection] elements
 */
sealed class ValuesGroupType : Parcelable {
    /**
     * Represents [Collection] which is empty or contains null value(s) only.
     */
    @Parcelize object None : ValuesGroupType()

    /**
     * Represents non-empty [Collection] containing different values.
     */
    @Parcelize object Mixed : ValuesGroupType()

    /**
     * Represents non-empty [Collection] containing the same elements.
     *
     * @param data Optional property allowing passing data. It's restricted to [Serializable] type to make it compatible
     * with [Parcelize].
     */
    @Parcelize data class Common(var data: Serializable? = null) : ValuesGroupType()
}

/**
 * Calculates group type of a [Collection]. In case the [Collection] has common values first of them is written
 * into [ValuesGroupType.Common.data] property.
 *
 * @return One of [ValuesGroupType] sub-types.
 */
fun <T : Serializable?> Collection<T>.groupType(): ValuesGroupType {
    return when {
        this.isEmpty() || filterNotNull().isEmpty() -> ValuesGroupType.None
        this.distinct().size == 1 -> ValuesGroupType.Common(first())
        else -> ValuesGroupType.Mixed
    }
}

fun formatPrice(value: BigDecimal, currency:String, isCurrencyPrefix: Boolean): String {
    return if (isCurrencyPrefix) {
        "$currency $value"
    } else {
        "$value $currency"
    }
}
