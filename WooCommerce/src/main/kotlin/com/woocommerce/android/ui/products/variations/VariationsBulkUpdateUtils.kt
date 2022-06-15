package com.woocommerce.android.ui.products.variations

import java.io.Serializable

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
