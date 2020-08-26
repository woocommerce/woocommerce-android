package com.woocommerce.android.ui.products

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

/**
 * Class provides the selection library access to stable selection keys and identifying items
 * presented by a [RecyclerView] instance.
 */
class ProductSelectionItemKeyProvider(private val recyclerView: RecyclerView) :
    ItemKeyProvider<Long>(SCOPE_MAPPED) {
    override fun getKey(position: Int): Long? {
        return recyclerView.adapter?.getItemId(position)
    }

    override fun getPosition(key: Long): Int {
        val viewHolder = recyclerView.findViewHolderForItemId(key)
        return viewHolder?.layoutPosition ?: RecyclerView.NO_POSITION
    }
}
