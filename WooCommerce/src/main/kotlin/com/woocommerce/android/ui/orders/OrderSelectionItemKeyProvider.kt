package com.woocommerce.android.ui.orders

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.orders.list.OrderListAdapter
import com.woocommerce.android.ui.orders.list.OrderListItemUIType

/**
 * Class provides the selection library access to stable selection keys and identifying items
 * presented by a [RecyclerView] instance.
 */
class OrderSelectionItemKeyProvider(private val recyclerView: RecyclerView) :
    ItemKeyProvider<Long>(SCOPE_MAPPED) {
    override fun getKey(position: Int): Long? {
        return (recyclerView.adapter as? OrderListAdapter)?.currentList?.get(position)?.let { item ->
            if (item is OrderListItemUIType.OrderListItemUI) item.orderId else null
        }
    }

    override fun getPosition(key: Long): Int =
        (recyclerView.adapter as? OrderListAdapter)?.orderIdAndPosition?.get(key) ?: RecyclerView.NO_POSITION
}
