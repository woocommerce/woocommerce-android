package com.woocommerce.android.widgets.tags

import android.content.Context

/**
 * Interface for working with individual Tag elements.
 */
abstract class ITag(val rawText: String) : Comparable<ITag> {
    /**
     * Returns the configuration to apply to this tag.
     */
    abstract fun getTagConfiguration(context: Context): TagConfig

    override fun equals(other: Any?): Boolean {
        return if (other !is ITag) {
            false
        } else {
            other.rawText == rawText
        }
    }

    override fun compareTo(other: ITag): Int {
        return rawText.toLowerCase().compareTo(other.rawText.toLowerCase())
    }

    override fun hashCode(): Int {
        return 38 + rawText.hashCode()
    }
}
