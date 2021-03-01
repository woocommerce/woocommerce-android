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
    private val dragHelper: ItemTouchHelper? = null
) : RecyclerView.Adapter<TermViewHolder>() {
    private var termNames = ArrayList<String>()

    init {
        setHasStableIds(true)
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

        holder.itemView.setOnClickListener {
            // TODO onItemClick(item)
        }
    }

    fun addTerm(termName: String): Boolean {
        termNames.forEach { term ->
            if (term.equals(termName, ignoreCase = true)) {
                return false
            }
        }

        termNames.add(0, termName)
        notifyItemInserted(0)
        return true
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

    fun setTerms(terms: List<String>) {
        val diffResult = DiffUtil.calculateDiff(
            TermItemDiffUtil(
                termNames,
                terms
            )
        )

        termNames.clear()
        termNames.addAll(terms)
        diffResult.dispatchUpdatesTo(this)
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
