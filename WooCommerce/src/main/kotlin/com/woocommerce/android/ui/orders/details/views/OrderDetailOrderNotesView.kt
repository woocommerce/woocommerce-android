package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailNoteListBinding
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.ui.orders.notes.OrderNotesAdapter
import com.woocommerce.android.widgets.SkeletonView

class OrderDetailOrderNotesView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailNoteListBinding.inflate(LayoutInflater.from(ctx), this)

    init {
        binding.notesListNotes.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = OrderNotesAdapter()
        }
    }

    private val skeletonView = SkeletonView()

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.notesListNotes, R.layout.skeleton_order_notes_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    fun updateOrderNotesView(
        orderNotes: List<OrderNote>,
        onTapAddOrderNote: () -> Unit
    ) {
        val adapter = binding.notesListNotes.adapter as? OrderNotesAdapter ?: OrderNotesAdapter()
        enableItemAnimator(adapter.itemCount == 0)

        val notesWithHeaders = addHeaders(orderNotes)
        adapter.setNotes(notesWithHeaders)

        binding.noteListAddNoteContainer.setOnClickListener { onTapAddOrderNote() }
    }

    private fun addHeaders(notes: List<OrderNote>): List<OrderNoteListItem> {
        return notes
            .groupBy(
                { DateFormat.getMediumDateFormat(context).format(it.dateCreated) },
                { Note(it) })
            .flatMap { listOf(Header(it.key)) + it.value }
    }

    private fun enableItemAnimator(enable: Boolean) {
        binding.notesListNotes.itemAnimator = if (enable) DefaultItemAnimator() else null
    }
}
