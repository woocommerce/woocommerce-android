package com.woocommerce.android.widgets.tags

import android.content.Context

/**
 * Interface for working with individual Tag elements.
 */
abstract class ITag(val rawText: String,
                    val bgColor: Int,
                    val fgColor: Int) : Comparable<ITag> {
    /**
     * Convert the raw text into a formatted label
     */
    abstract fun getFormattedLabel(context: Context): String

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
