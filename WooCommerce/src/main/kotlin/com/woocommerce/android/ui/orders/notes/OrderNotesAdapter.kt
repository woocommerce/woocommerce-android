package com.woocommerce.android.ui.orders.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.woocommerce.android.databinding.OrderDetailNoteListHeaderBinding
import com.woocommerce.android.databinding.OrderDetailNoteListNoteBinding
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.ViewType

class OrderNotesAdapter : Adapter<OrderNoteViewHolder>() {
    private val notes = mutableListOf<OrderNoteListItem>()

    init {
        setHasStableIds(true)
    }

    fun setNotes(newList: List<OrderNoteListItem>) {
        val diffResult = DiffUtil.calculateDiff(OrderNotesDiffCallback(notes.toList(), newList))
        notes.clear()
        notes.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderNoteViewHolder {
        val inflater = LayoutInflater.from(parent.getContext())
        return when (viewType) {
            ViewType.NOTE.id -> {
                NoteItemViewHolder(
                    OrderDetailNoteListNoteBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            ViewType.HEADER.id -> {
                HeaderItemViewHolder(
                    OrderDetailNoteListHeaderBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            else -> throw IllegalArgumentException("Unexpected view type in OrderNotesAdapter")
        }
    }

    override fun onBindViewHolder(holder: OrderNoteViewHolder, position: Int) {
        val isLast = position == notes.size - 1
        when (getItemViewType(position)) {
            ViewType.NOTE.id -> (holder as NoteItemViewHolder).bind(notes[position] as Note, isLast)
            ViewType.HEADER.id -> (holder as HeaderItemViewHolder).bind(notes[position] as Header)
            else -> throw IllegalArgumentException("Unexpected view holder in OrderNotesAdapter")
        }
    }

    override fun getItemCount() = notes.size

    override fun getItemId(position: Int): Long = notes[position].longId

    override fun getItemViewType(position: Int): Int {
        return notes[position].viewType.id
    }
}
