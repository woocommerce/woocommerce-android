package com.woocommerce.android.ui.products

import androidx.recyclerview.widget.DiffUtil
import com.woocommerce.android.model.Product

object ProductItemDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(
        oldItem: Product,
        newItem: Product
    ): Boolean {
        return oldItem.remoteId == newItem.remoteId
    }

    override fun areContentsTheSame(
        oldItem: Product,
        newItem: Product
    ): Boolean {
        return oldItem == newItem
    }
}
