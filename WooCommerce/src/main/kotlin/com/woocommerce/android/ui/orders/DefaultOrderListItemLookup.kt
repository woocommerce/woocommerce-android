package com.woocommerce.android.ui.orders

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.orders.list.OrderListAdapter
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI

class DefaultOrderListItemLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? =
        recyclerView
            .findChildViewUnder(event.x, event.y)
            ?.let { view -> getDetailsFromView(view) }

    private fun getDetailsFromView(view: View): ItemDetails<Long>? {
        val viewHolder = recyclerView.getChildViewHolder(view) ?: return null
        val position = viewHolder.bindingAdapterPosition

        return when {
            position == RecyclerView.NO_POSITION -> null
            else -> {
                val item = (recyclerView.adapter as? OrderListAdapter)?.currentList?.get(position)
                if (item is OrderListItemUI) {
                    DefaultOrderItemDetailsLookup(position, item.orderId)
                } else {
                    null
                }
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
