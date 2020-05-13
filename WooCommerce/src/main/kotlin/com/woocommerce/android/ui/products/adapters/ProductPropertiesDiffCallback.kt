package com.woocommerce.android.ui.products.adapters

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable

class ProductPropertiesDiffCallback(
    private val oldList: List<ProductProperty>,
    private val newList: List<ProductProperty>
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

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem is Editable && newItem is Editable && newItem.text != oldItem.text) {
            newItem.shouldFocus = true
        } else null
    }
}
