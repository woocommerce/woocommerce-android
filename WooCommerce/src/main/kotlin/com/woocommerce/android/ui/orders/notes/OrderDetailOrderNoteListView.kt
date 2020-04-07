package com.woocommerce.android.ui.orders.notes

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.order_detail_note_list.view.*

class OrderDetailOrderNoteListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_note_list, this)
    }

    interface OrderDetailNoteListener {
        fun onRequestAddNote()
    }

    private val skeletonView = SkeletonView()
    private lateinit var listener: OrderDetailNoteListener

    fun initView(notes: List<OrderNote>, orderDetailListener: OrderDetailNoteListener) {
        listener = orderDetailListener

        noteList_addNoteContainer.setOnClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED)

            listener.onRequestAddNote()
        }

        notesList_notes.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = OrderNotesAdapter()
        }

        updateView(notes)
    }

    private fun addHeaders(notes: List<OrderNote>): List<OrderNoteListItem> {
        return notes
                .groupBy(
                        { DateFormat.getMediumDateFormat(context).format(it.dateCreated) },
                        { Note(it) })
                .flatMap { listOf(Header(it.key)) + it.value }
    }

    fun updateView(notes: List<OrderNote>) {
        val adapter = notesList_notes.adapter as? OrderNotesAdapter ?: OrderNotesAdapter()
        enableItemAnimator(adapter.itemCount == 0)

        val notesWithHeaders = addHeaders(notes)
        adapter.setNotes(notesWithHeaders)
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(notesList_notes, R.layout.skeleton_order_notes_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    private fun enableItemAnimator(enable: Boolean) {
        notesList_notes.itemAnimator = if (enable) DefaultItemAnimator() else null
    }
}
