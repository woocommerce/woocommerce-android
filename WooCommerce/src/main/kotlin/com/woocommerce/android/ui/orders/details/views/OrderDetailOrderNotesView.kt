package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.ui.orders.notes.OrderNotesAdapter
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.order_detail_note_list.view.*

class OrderDetailOrderNotesView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_note_list, this)
        notesList_notes.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = OrderNotesAdapter()
        }
    }

    private val skeletonView = SkeletonView()

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(notesList_notes, R.layout.skeleton_order_notes_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    fun updateOrderNotesView(orderNotes: List<OrderNote>) {
        val adapter = notesList_notes.adapter as? OrderNotesAdapter ?: OrderNotesAdapter()
        enableItemAnimator(adapter.itemCount == 0)

        val notesWithHeaders = addHeaders(orderNotes)
        adapter.setNotes(notesWithHeaders)
    }

    private fun addHeaders(notes: List<OrderNote>): List<OrderNoteListItem> {
        return notes
            .groupBy(
                { DateFormat.getMediumDateFormat(context).format(it.dateCreated) },
                { Note(it) })
            .flatMap { listOf(Header(it.key)) + it.value }
    }

    private fun enableItemAnimator(enable: Boolean) {
        notesList_notes.itemAnimator = if (enable) DefaultItemAnimator() else null
    }
}
