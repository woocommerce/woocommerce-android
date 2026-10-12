package com.woocommerce.android.ui.products.variations.attributes

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeItemBinding
import com.woocommerce.android.model.ProductAttribute
import java.util.Objects

abstract class AttributeBaseAdapter<T : AttributeBaseAdapter.AttributeBaseViewHolder>(
    private val onItemClick: (attributeId: Long, attributeName: String) -> Unit
) : RecyclerView.Adapter<T>() {
    private var attributeList = listOf<ProductAttribute>()

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: T, position: Int) {
        holder.bind(attributeList[position])

        holder.itemView.setOnClickListener {
            val item = attributeList[position]
            onItemClick(item.id, item.name)
        }
    }

    // local attributes all have id 0, so the name is needed to tell them apart
    override fun getItemId(position: Int) = attributeList[position].let { Objects.hash(it.id, it.name).toLong() }

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
