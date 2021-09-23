package com.woocommerce.android.ui.products.addons.options

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductAddonOptionItemBinding
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter.AddonOptionsViewHolder
import com.woocommerce.android.ui.products.addons.toFormattedPrice
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
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
            binding.optionPrice.text = when(option.price.priceType) {
                FlatFee -> option.price.toFormattedPrice(formatCurrencyForDisplay)
                PercentageBased -> "%${option.price}"
                else -> option.price.value
            }
        }
    }
}
