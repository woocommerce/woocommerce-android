package com.woocommerce.android.ui.products.addons.options

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductAddonOptionItemBinding
import com.woocommerce.android.model.ProductAddonOption
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter.AddonOptionsViewHolder

class AddonOptionListAdapter(
    val options: List<ProductAddonOption>
) : RecyclerView.Adapter<AddonOptionsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AddonOptionsViewHolder(
            ProductAddonOptionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: AddonOptionsViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size

    inner class AddonOptionsViewHolder(
        val binding: ProductAddonOptionItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(option: ProductAddonOption) {
            binding.optionName.text = option.label
            binding.optionPrice.text = option.price
        }
    }
}
