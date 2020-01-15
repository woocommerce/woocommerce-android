package com.woocommerce.android.util

import android.content.res.TypedArray
import androidx.annotation.StyleableRes

/**
 * Helper functions for working with custom view attributes and styles
 */
object StyleAttrUtils {
    fun getString(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int
    ): String? {
        return a.getString(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr)
    }

    fun getBoolean(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Boolean
    ): Boolean {
        return a.getBoolean(
                if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr,
                defVal
        )
    }
}
