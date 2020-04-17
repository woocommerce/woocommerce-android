package com.woocommerce.android.ui.orders.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

abstract class OrderNoteViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
        LayoutInflater.from(
                parent.context
        ).inflate(layout, parent, false)
)

class HeaderItemViewHolder(parent: ViewGroup) : OrderNoteViewHolder(parent, R.layout.order_detail_note_list_header) {
    private val header: MaterialTextView = itemView.findViewById(R.id.orderDetail_noteListHeader)

    fun bind(item: OrderNoteListItem.Header) {
        header.text = item.text
    }
}

class NoteItemViewHolder(parent: ViewGroup) : OrderNoteViewHolder(parent, R.layout.order_detail_note_list_note) {
    private val noteItem = itemView as OrderDetailOrderNoteItemView
    fun bind(item: OrderNoteListItem.Note, isLast: Boolean) {
        noteItem.initView(item.note, isLast)
    }
}
