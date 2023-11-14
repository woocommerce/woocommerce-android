package com.woocommerce.android.ui.products.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.databinding.BundledProductItemViewBinding
import com.woocommerce.android.model.BundledProduct

class BundleProductListAdapter : ListAdapter<BundledProduct, BundleProductViewHolder>(BundledProductItemDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BundleProductViewHolder {
        return BundleProductViewHolder(
            BundledProductItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BundleProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }
}

object BundledProductItemDiffCallback : DiffUtil.ItemCallback<BundledProduct>() {
    override fun areItemsTheSame(
        oldItem: BundledProduct,
        newItem: BundledProduct
    ): Boolean {
        return oldItem.bundledProductId == newItem.bundledProductId &&
            oldItem.id == newItem.id &&
            oldItem.parentProductId == newItem.parentProductId
    }

    override fun areContentsTheSame(
        oldItem: BundledProduct,
        newItem: BundledProduct
    ): Boolean {
        return oldItem == newItem
    }
}
