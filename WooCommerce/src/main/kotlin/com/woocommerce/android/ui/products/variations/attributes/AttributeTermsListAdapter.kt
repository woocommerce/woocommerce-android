package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeTermListItemBinding
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.TermViewHolder

/**
 * Adapter which shows a simple list of attribute term names
 */
class AttributeTermsListAdapter() : RecyclerView.Adapter<TermViewHolder>() {
    private var termNames = listOf<String>()

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

        termNames = terms
        diffResult.dispatchUpdatesTo(this)
    }

    inner class TermViewHolder(val viewBinding: AttributeTermListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(term: String) {
            viewBinding.termName.text = term
        }
    }
}
