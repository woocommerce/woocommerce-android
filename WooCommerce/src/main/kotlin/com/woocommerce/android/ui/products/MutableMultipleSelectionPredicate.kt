package com.woocommerce.android.ui.products

import androidx.recyclerview.selection.SelectionTracker

class MutableMultipleSelectionPredicate<K : Any> : SelectionTracker.SelectionPredicate<K>() {
    var selectMultiple = true

    override fun canSetStateForKey(key: K, nextState: Boolean): Boolean = true

    override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean = true

    override fun canSelectMultiple(): Boolean = selectMultiple
}
