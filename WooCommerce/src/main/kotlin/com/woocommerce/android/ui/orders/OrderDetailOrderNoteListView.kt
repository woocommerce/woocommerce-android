package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.widgets.AlignedDividerDecoration
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

    private lateinit var listener: OrderDetailNoteListener

    fun initView(notes: List<WCOrderNoteModel>, orderDetailListener: OrderDetailNoteListener) {
        listener = orderDetailListener

        if (notes.isNotEmpty()) {
            notesList_progress.visibility = View.GONE
        }

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = OrderNotesAdapter(notes.toMutableList())
        val divider = AlignedDividerDecoration(context,
                DividerItemDecoration.VERTICAL, R.id.orderNote_created, clipToMargin = false)

        ContextCompat.getDrawable(context, R.drawable.list_divider)?.let { drawable ->
            divider.setDrawable(drawable)
        }

        ContextCompat.getDrawable(context, R.drawable.ic_gridicons_add_outline_purple)?.let { drawable ->
            noteList_addNote.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }
        noteList_addNote.setOnClickListener {
            listener.onRequestAddNote()
        }

        notesList_notes.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(divider)
            adapter = viewAdapter
        }
    }

    fun updateView(notes: List<WCOrderNoteModel>) {
        notesList_progress.visibility = View.VISIBLE

        val adapter = notesList_notes.adapter as OrderNotesAdapter
        adapter.setNotes(notes)

        notesList_progress.visibility = View.GONE
    }

    class OrderNotesAdapter(private val notes: MutableList<WCOrderNoteModel>)
        : RecyclerView.Adapter<OrderNotesAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailOrderNoteItemView) : RecyclerView.ViewHolder(view)

        fun setNotes(newList: List<WCOrderNoteModel>) {
            if (newList != notes) {
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
    }
}
