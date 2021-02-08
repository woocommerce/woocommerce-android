package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.ui.products.variations.attributes.CombinedAttributeListAdapter.AttributeViewHolder

class CombinedAttributeListAdapter(
    private val onItemClick: (id: Long, isGlobalAttribute: Boolean) -> Unit
) : RecyclerView.Adapter<AttributeViewHolder>() {
    private var attributeList = listOf<ProductCombinedAttribute>()

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
            val item = attributeList[position]
            onItemClick(item.id, item.isGlobalAttribute)
        }
    }

    private class AttributeItemDiffUtil(
        val oldList: List<ProductCombinedAttribute>,
        val newList: List<ProductCombinedAttribute>
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

    fun setAttributeList(
        localAttributes: List<Product.Attribute>? = null,
        globalAttributes: List<ProductGlobalAttribute>? = null
    ) {
        val combinedList = ArrayList<ProductCombinedAttribute>()

        localAttributes?.let { localList ->
            localList.map { combinedList.add(ProductCombinedAttribute.fromLocalAttribute(it)) }
        }

        globalAttributes?.let { globalList ->
            globalList.map { combinedList.add(ProductCombinedAttribute.fromGlobalAttribute(it)) }
        }

        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                attributeList,
                combinedList
            )
        )

        attributeList = combinedList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: ProductCombinedAttribute) {
            viewBinding.attributeName.text = attribute.name
            if (attribute.commaSeparatedOptions.isNotEmpty()) {
                viewBinding.attributeTerms.isVisible = true
                viewBinding.attributeTerms.text = attribute.commaSeparatedOptions
            } else {
                viewBinding.attributeTerms.isVisible = false
            }
        }
    }
}
