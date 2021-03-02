package com.woocommerce.android.ui.products.variations.attributes

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeTermListItemBinding
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.TermViewHolder

/**
 * Adapter which shows a simple list of attribute term names
 */
class AttributeTermsListAdapter(
    private val showIcons: Boolean,
    private val dragHelper: ItemTouchHelper? = null,
    private val onTermClick: OnTermClickListener? = null
) : RecyclerView.Adapter<TermViewHolder>() {
    interface OnTermClickListener {
        fun onTermClick(termName: String)
    }

    var termNames: ArrayList<String> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                TermItemDiffUtil(
                    field,
                    value
                ), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount() = termNames.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        return TermViewHolder(
            AttributeTermListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        holder.bind(termNames[position])

        onTermClick?.let { listener ->
            holder.itemView.setOnClickListener {
                val item = termNames[position]
                listener.onTermClick(item)
            }
        }
    }

    fun isEmpty() = termNames.isEmpty()

    fun clear() {
        if (!isEmpty()) {
            termNames.clear()
            notifyDataSetChanged()
        }
    }

    private fun containsTerm(termName: String): Boolean {
        termNames.forEach { term ->
            if (term.equals(termName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun addTerm(termName: String) {
        if (!containsTerm(termName)) {
            termNames.add(0, termName)
            notifyItemInserted(0)
        }
    }

    fun swapItems(from: Int, to: Int) {
        val fromValue = termNames[from]
        termNames[from] = termNames[to]
        termNames[to] = fromValue
        notifyItemChanged(from)
        notifyItemChanged(to)
    }

    private fun removeTerm(term: String) {
        val index = termNames.indexOf(term)
        if (index >= 0) {
            termNames.remove(term)
            notifyItemRemoved(index)
        }
    }

    private class TermItemDiffUtil(
        val oldList: List<String>,
        val newList: List<String>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }

    inner class TermViewHolder(val viewBinding: AttributeTermListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(term: String) {
            viewBinding.termName.text = term
            viewBinding.termDragHandle.isVisible = showIcons
            viewBinding.termDelete.isVisible = showIcons

            if (showIcons) {
                viewBinding.termDelete.setOnClickListener {
                    removeTerm(term)
                }

                viewBinding.termDragHandle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        dragHelper?.startDrag(this)
                    }
                    false
                }
            }
        }
    }
}
