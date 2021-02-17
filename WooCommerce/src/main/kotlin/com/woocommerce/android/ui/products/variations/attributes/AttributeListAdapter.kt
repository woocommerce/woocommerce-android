package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.variations.attributes.AttributeListAdapter.AttributeViewHolder

class AttributeListAdapter(
    private val onItemClick: (attribute: ProductAttribute) -> Unit
) : RecyclerView.Adapter<AttributeViewHolder>() {
    private var attributeList = listOf<ProductAttribute>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = attributeList[position].id

    override fun getItemCount() = attributeList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributeViewHolder {
        return AttributeViewHolder(
            AttributeListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AttributeViewHolder, position: Int) {
        holder.bind(attributeList[position])

        holder.itemView.setOnClickListener {
            onItemClick(attributeList[position])
        }
    }

    private class AttributeItemDiffUtil(
        val oldList: List<ProductAttribute>,
        val newList: List<ProductAttribute>
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

    fun setAttributeList(attributes: List<ProductAttribute>) {
        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                attributeList,
                attributes
            )
        )
        attributeList = attributes
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: ProductAttribute) {
            viewBinding.attributeName.text = attribute.name
            if (attribute.terms.isNotEmpty()) {
                viewBinding.attributeTerms.isVisible = true
                viewBinding.attributeTerms.text = attribute.terms.joinToString()
            } else {
                viewBinding.attributeTerms.isVisible = false
            }
        }
    }
}
