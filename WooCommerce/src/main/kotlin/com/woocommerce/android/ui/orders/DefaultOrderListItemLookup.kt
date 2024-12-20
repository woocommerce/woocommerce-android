package com.woocommerce.android.ui.orders

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.orders.list.OrderListAdapter
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI

class DefaultOrderListItemLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? =
        recyclerView
            .findChildViewUnder(event.x, event.y)
            ?.let { view ->
                recyclerView.getChildViewHolder(view)?.let { viewHolder ->
                    val position = viewHolder.bindingAdapterPosition
                    val item = (recyclerView.adapter as? OrderListAdapter)?.currentList?.get(position)
                    if (item is OrderListItemUI) {
                        DefaultOrderItemDetailsLookup(position, item.orderId)
                    } else {
                        null
                    }
                }
            }
}

class DefaultOrderItemDetailsLookup(
    private val position: Int,
    private val orderId: Long
) : ItemDetailsLookup.ItemDetails<Long>() {
    override fun getPosition() = position
    override fun getSelectionKey() = orderId
}

class SelectableOrderItemDetailsLookup(
    private val position: Int,
    private val orderId: Long
) : ItemDetailsLookup.ItemDetails<Long>() {
    override fun getPosition() = position
    override fun getSelectionKey() = orderId
    override fun inSelectionHotspot(e: MotionEvent) = true
}
