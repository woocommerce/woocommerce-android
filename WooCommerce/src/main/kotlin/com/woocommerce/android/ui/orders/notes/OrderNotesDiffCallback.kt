package com.woocommerce.android.ui.orders.notes

import androidx.recyclerview.widget.DiffUtil
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note

class OrderNotesDiffCallback(
    private val oldList: List<OrderNoteListItem>,
    private val newList: List<OrderNoteListItem>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is Header && newItem is Header -> oldItem.text == newItem.text
            oldItem is Note && newItem is Note -> oldItem.note.remoteNoteId == newItem.note.remoteNoteId
            else -> false
        }
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
