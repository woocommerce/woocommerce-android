package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.ui.products.variations.attributes.GlobalAttributeListAdapter.GlobalAttributeViewHolder

class GlobalAttributeListAdapter(
    private val onItemClick: (attribute: ProductGlobalAttribute) -> Unit
) : RecyclerView.Adapter<GlobalAttributeViewHolder>() {
    private var globalAttributeList = listOf<ProductGlobalAttribute>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = globalAttributeList[position].id.toLong()

    override fun getItemCount() = globalAttributeList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlobalAttributeViewHolder {
        return GlobalAttributeViewHolder(
            // note that this uses the same list item view as AttributeListAdapter
            AttributeListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: GlobalAttributeViewHolder, position: Int) {
        holder.bind(globalAttributeList[position])

        holder.itemView.setOnClickListener {
            onItemClick(globalAttributeList[position])
        }
    }

    private class AttributeItemDiffUtil(
        val oldList: List<ProductGlobalAttribute>,
        val newList: List<ProductGlobalAttribute>
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

    fun setGlobalAttributeList(attributes: List<ProductGlobalAttribute>) {
        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                globalAttributeList,
                attributes
            )
        )
        globalAttributeList = attributes
        diffResult.dispatchUpdatesTo(this)
    }

    inner class GlobalAttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: ProductGlobalAttribute) {
            viewBinding.attributeName.text = attribute.name
            viewBinding.attributeTerms.isVisible = false
        }
    }
}
