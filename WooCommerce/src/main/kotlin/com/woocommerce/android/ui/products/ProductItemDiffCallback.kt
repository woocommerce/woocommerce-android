package com.woocommerce.android.ui.products

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.woocommerce.android.model.Product

object ProductItemDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(
        oldItem: Product,
        newItem: Product
    ): Boolean {
        return oldItem.remoteId == newItem.remoteId && oldItem.isSelected == newItem.isSelected
    }

    override fun areContentsTheSame(
        oldItem: Product,
        newItem: Product
    ): Boolean {
//        Log.d("AAA", "Are contents the same: " + (oldItem.isSelected == newItem.isSelected).toString())
        return oldItem == newItem && oldItem.isSelected == newItem.isSelected
    }
}
