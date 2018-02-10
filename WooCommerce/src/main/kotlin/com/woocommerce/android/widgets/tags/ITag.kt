package com.woocommerce.android.widgets.tags

import android.content.Context
import android.graphics.Color

/**
 * Interface for working with individual Tag elements.
 */
abstract class ITag(val rawText: String) : Comparable<ITag> {
    /**
     * Configurable style attributes
     */
    var fgColor: Int = Color.GRAY
    var bgColor: Int = Color.LTGRAY

    /**
     * Convert the raw text into a formatted label
     */
    abstract fun getFormattedLabel(context: Context): String

    /**
     * Customize the style of this tag.
     * @param [foregroundColor] The color to apply to the text
     * @param [backgroundColor] The color to apply to the background
     */
    @Suppress("unused")
    fun setColors(foregroundColor: Int, backgroundColor: Int) {
        fgColor = foregroundColor
        bgColor = backgroundColor
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ITag) false
        else other.rawText == rawText
                && other.fgColor == fgColor
                && other.bgColor == bgColor
    }

    override fun compareTo(other: ITag): Int {
        return rawText.toLowerCase().compareTo(other.rawText.toLowerCase())
    }

    override fun hashCode(): Int {
        return 38 + fgColor.hashCode() + rawText.hashCode() + bgColor.hashCode()
    }
}
