package com.woocommerce.android.ui.products.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductItemDiffCallback
import com.woocommerce.android.ui.products.ProductItemViewHolder

class SimpleProductListAdapter : ListAdapter<Product, ProductItemViewHolder>(ProductItemDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).remoteId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductItemViewHolder {
        return ProductItemViewHolder(
            ProductListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }
}
