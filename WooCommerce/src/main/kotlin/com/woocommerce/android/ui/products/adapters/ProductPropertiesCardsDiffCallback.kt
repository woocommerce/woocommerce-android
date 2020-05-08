package com.woocommerce.android.ui.products.adapters

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.products.models.ProductPropertyCard

class ProductPropertiesCardsDiffCallback(
    private val oldList: List<ProductPropertyCard>,
    private val newList: List<ProductPropertyCard>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.type == newItem.type
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
