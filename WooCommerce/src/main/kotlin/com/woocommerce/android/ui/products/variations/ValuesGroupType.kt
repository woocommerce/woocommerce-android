package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Names states of [Collection] elements.
 */
sealed class ValuesGroupType : Parcelable {
    /**
     * Represents [Collection] which is empty or contains null value(s) only.
     */
    @Parcelize
    object None : ValuesGroupType()

    /**
     * Represents non-empty [Collection] containing different values.
     */
    @Parcelize
    object Mixed : ValuesGroupType()

    /**
     * Represents non-empty [Collection] containing the same elements.
     *
     * @param data Optional property allowing passing data. It's restricted to [Serializable] type to make it compatible
     * with [Parcelize].
     */
    @Parcelize
    data class Common(var data: Serializable? = null) : ValuesGroupType()
}
