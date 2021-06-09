package com.woocommerce.android.ui.products.variations.attributes

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.model.ProductAttribute

abstract class AttributeBaseAdapter<T : RecyclerView.ViewHolder> : RecyclerView.Adapter<T>() {
    var attributeList = listOf<ProductAttribute>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = attributeList[position].id

    override fun getItemCount() = attributeList.size

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
}
