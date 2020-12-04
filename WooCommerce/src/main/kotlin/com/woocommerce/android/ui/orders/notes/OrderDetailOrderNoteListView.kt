package com.woocommerce.android.ui.orders.notes

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.OrderDetailNoteListBinding
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.widgets.SkeletonView

class OrderDetailOrderNoteListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailNoteListBinding.inflate(LayoutInflater.from(ctx), this)

    interface OrderDetailNoteListener {
        fun onRequestAddNote()
    }

    private val skeletonView = SkeletonView()
    private lateinit var listener: OrderDetailNoteListener

    fun initView(notes: List<OrderNote>, orderDetailListener: OrderDetailNoteListener) {
        listener = orderDetailListener

        binding.noteListAddNoteContainer.setOnClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED)

            listener.onRequestAddNote()
        }

        binding.notesListNotes.apply {
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
        val adapter = binding.notesListNotes.adapter as? OrderNotesAdapter ?: OrderNotesAdapter()
        enableItemAnimator(adapter.itemCount == 0)

        val notesWithHeaders = addHeaders(notes)
        adapter.setNotes(notesWithHeaders)
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.notesListNotes, R.layout.skeleton_order_notes_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    private fun enableItemAnimator(enable: Boolean) {
        binding.notesListNotes.itemAnimator = if (enable) DefaultItemAnimator() else null
    }
}
