package com.woocommerce.android.ui.products

import androidx.recyclerview.selection.SelectionTracker

class MutableMultipleSelectionPredicate<K : Any>(
    private val maxSelectionCount: Int? = null
) : SelectionTracker.SelectionPredicate<K>() {
    var selectMultiple = true
    var currentSelectionCount = 0

    override fun canSetStateForKey(key: K, nextState: Boolean): Boolean {
        val limit = maxSelectionCount ?: return true
        return !(nextState && currentSelectionCount >= limit)
    }

    override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean = true

    override fun canSelectMultiple(): Boolean = selectMultiple
}
