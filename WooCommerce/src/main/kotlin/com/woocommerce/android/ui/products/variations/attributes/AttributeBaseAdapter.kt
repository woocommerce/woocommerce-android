package com.woocommerce.android.ui.products.variations.attributes

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeItemBinding
import com.woocommerce.android.model.ProductAttribute

abstract class AttributeBaseAdapter<T : AttributeBaseAdapter.AttributeBaseViewHolder>(
    private val onItemClick: (attributeId: Long, attributeName: String) -> Unit
) : RecyclerView.Adapter<T>() {
    private var attributeList = listOf<ProductAttribute>()

    override fun onBindViewHolder(holder: T, position: Int) {
        holder.bind(attributeList[position])

        holder.itemView.setOnClickListener {
            attributeList.getOrNull(holder.bindingAdapterPosition)?.let { onItemClick(it.id, it.name) }
        }
    }

    override fun getItemCount() = attributeList.size

    private class AttributeItemDiffUtil(
        val oldList: List<ProductAttribute>,
        val newList: List<ProductAttribute>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id &&
                oldList[oldItemPosition].name == newList[newItemPosition].name

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun refreshAttributeList(attributes: List<ProductAttribute>) {
        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                attributeList,
                attributes
            )
        )
        attributeList = attributes
        diffResult.dispatchUpdatesTo(this)
    }

    abstract class AttributeBaseViewHolder(val viewBinding: AttributeItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        abstract fun bind(attribute: ProductAttribute)
    }
}
