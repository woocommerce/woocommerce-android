package com.woocommerce.android.util

import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.AnyRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.StyleableRes

/**
 * Helper functions for working with custom view attributes and styles.
 */
object StyleAttrUtils {
    fun getString(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int
    ): String? = a.getString(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr)

    fun getBoolean(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Boolean = false
    ) = a.getBoolean(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getText(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int
    ): CharSequence? = a.getText(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr)

    fun getInt(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Int
    ) = a.getInt(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getFloat(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Float
    ) = a.getFloat(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    @ColorInt
    fun getColor(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        @ColorInt defVal: Int
    ) = a.getColor(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getInteger(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Int
    ) = a.getInteger(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getDimension(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Float
    ) = a.getDimension(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getDimensionPixelOffset(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Int
    ) = a.getDimensionPixelOffset(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getDimensionPixelSize(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        defVal: Int
    ) = a.getDimensionPixelSize(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    @AnyRes
    fun getResourceId(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int,
        @AnyRes defVal: Int = 0
    ) = a.getResourceId(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr, defVal)

    fun getDrawable(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int
    ): Drawable? = a.getDrawable(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr)

    @RequiresApi(26)
    fun getFont(
        a: TypedArray,
        isInEditMode: Boolean,
        @StyleableRes appAttr: Int,
        @StyleableRes toolsAttr: Int
    ): Typeface? = a.getFont(if (isInEditMode && a.hasValue(toolsAttr)) toolsAttr else appAttr)
}
