package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.order_detail_note_list.view.*
import org.wordpress.android.fluxc.model.WCOrderNoteModel

class OrderDetailOrderNoteListView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_note_list, this)
    }

    interface OrderDetailNoteListener {
        fun onRequestAddNote()
    }

    private val skeletonView = SkeletonView()
    private lateinit var listener: OrderDetailNoteListener

    // negative IDs denote transient notes
    private var nextTransientNoteId = -1

    fun initView(notes: List<WCOrderNoteModel>, orderDetailListener: OrderDetailNoteListener) {
        listener = orderDetailListener

        val viewManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        val viewAdapter = OrderNotesAdapter(notes.toMutableList())
        val divider = AlignedDividerDecoration(context,
                DividerItemDecoration.VERTICAL, R.id.orderNote_created, clipToMargin = false)

        ContextCompat.getDrawable(context, R.drawable.list_divider)?.let { drawable ->
            divider.setDrawable(drawable)
        }

        noteList_addNoteContainer.setOnClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED)

            listener.onRequestAddNote()
        }

        notesList_notes.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            addItemDecoration(divider)
            adapter = viewAdapter
        }
    }

    fun updateView(notes: List<WCOrderNoteModel>) {
        val adapter = notesList_notes.adapter as OrderNotesAdapter
        enableItemAnimator(adapter.itemCount == 0)
        adapter.setNotes(notes)
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(notesList_notes, R.layout.skeleton_order_notes_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    /*
     * a transient note is a temporary placeholder created after the user adds a note but before the request to
     * add the note has completed - this enables us to be optimistic about connectivity
     */
    fun addTransientNote(noteText: String, isCustomerNote: Boolean) {
        enableItemAnimator(true)
        val noteModel = WCOrderNoteModel(nextTransientNoteId)
        noteModel.note = noteText
        noteModel.isCustomerNote = isCustomerNote
        (notesList_notes.adapter as OrderNotesAdapter).addNote(noteModel)
        nextTransientNoteId--
        notesList_notes.scrollToPosition(0)
    }

    private fun enableItemAnimator(enable: Boolean) {
        notesList_notes.itemAnimator = if (enable) androidx.recyclerview.widget.DefaultItemAnimator() else null
    }

    class OrderNotesAdapter(private val notes: MutableList<WCOrderNoteModel>)
        : RecyclerView.Adapter<OrderNotesAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailOrderNoteItemView) : RecyclerView.ViewHolder(view)

        init {
            setHasStableIds(true)
        }

        fun setNotes(newList: List<WCOrderNoteModel>) {
            if (!isSameNoteList(newList)) {
                notes.clear()
                notes.addAll(newList)
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_note_list_item, parent, false)
                    as OrderDetailOrderNoteItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.initView(notes[position])
        }

        override fun getItemCount() = notes.size

        override fun getItemId(position: Int): Long = notes[position].id.toLong()

        fun addNote(noteModel: WCOrderNoteModel) {
            notes.add(0, noteModel)
            notifyItemInserted(0)
        }

        private fun isSameNoteList(otherNotes: List<WCOrderNoteModel>): Boolean {
            if (otherNotes.size != notes.size) {
                return false
            }

            for (i in 0 until notes.size) {
                val thisNote = notes[i]
                val thatNote = otherNotes[i]
                if (thisNote.localOrderId != thatNote.localOrderId ||
                        thisNote.localSiteId != thatNote.localSiteId ||
                        thisNote.remoteNoteId != thatNote.remoteNoteId ||
                        thisNote.isCustomerNote != thatNote.isCustomerNote ||
                        thisNote.note != thatNote.note ||
                        thisNote.dateCreated != thatNote.dateCreated) {
                    return false
                }
            }

            return true
        }
    }
}
