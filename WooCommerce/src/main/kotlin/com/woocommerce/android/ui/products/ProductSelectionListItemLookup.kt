package com.woocommerce.android.ui.products

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

/**
 * Class that provides the recyclerview selection library the information about the
 * items associated with the users selection.
 *
 * That selection is based on a MotionEvent that we have mapped to the [ProductItemViewHolder].
 * Given that motion event, we will find the child of the RecyclerView where the event happened and
 * return the details of that item.
 */
class ProductSelectionListItemLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? =
        recyclerView
            .findChildViewUnder(event.x, event.y)
            ?.let { recyclerView.getChildViewHolder(it) as? ProductItemViewHolder }
            ?.getItemDetails()
}
