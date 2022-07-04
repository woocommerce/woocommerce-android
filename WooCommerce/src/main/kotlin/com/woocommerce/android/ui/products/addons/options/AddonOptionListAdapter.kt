package com.woocommerce.android.ui.products.addons.options

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductAddonOptionItemBinding
import com.woocommerce.android.ui.products.addons.handlePriceType
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter.AddonOptionsViewHolder
import org.wordpress.android.fluxc.domain.Addon
import java.math.BigDecimal

class AddonOptionListAdapter(
    private val options: List<Addon.HasOptions.Option>,
    private val formatCurrencyForDisplay: (BigDecimal) -> String
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
        fun bind(option: Addon.HasOptions.Option) {
            binding.optionName.text = option.label
            binding.optionPrice.text = option.price.handlePriceType(formatCurrencyForDisplay)
        }
    }
}
