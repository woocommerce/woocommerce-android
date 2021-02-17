package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeTermListItemBinding
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.TermViewHolder

class AttributeTermsListAdapter() : RecyclerView.Adapter<TermViewHolder>() {
    private var termsList = listOf<ProductAttributeTerm>()

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = termsList.size

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
        holder.bind(termsList[position])

        holder.itemView.setOnClickListener {
            // TODO onItemClick(item)
        }
    }

    private class TermItemDiffUtil(
        val oldList: List<ProductAttributeTerm>,
        val newList: List<ProductAttributeTerm>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun setTermsList(terms: List<ProductAttributeTerm>) {
        val diffResult = DiffUtil.calculateDiff(
            TermItemDiffUtil(
                termsList,
                terms
            )
        )

        termsList = terms
        diffResult.dispatchUpdatesTo(this)
    }

    inner class TermViewHolder(val viewBinding: AttributeTermListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(term: ProductAttributeTerm) {
            viewBinding.termName.text = term.name
        }
    }
}
