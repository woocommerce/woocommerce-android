package com.woocommerce.android.ui.products

import androidx.recyclerview.widget.DiffUtil
import com.woocommerce.android.model.Product

class ProductItemDiffUtil(val items: List<Product>, val result: List<Product>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        items[oldItemPosition].remoteId == result[newItemPosition].remoteId

    override fun getOldListSize(): Int = items.size

    override fun getNewListSize(): Int = result.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = items[oldItemPosition]
        val newItem = result[newItemPosition]
        return oldItem.isSameProduct(newItem)
    }
}
