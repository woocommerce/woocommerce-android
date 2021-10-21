package com.woocommerce.android.ui.products.addons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductAddonCardBinding
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter
import com.woocommerce.android.widgets.AlignedDividerDecoration
import org.wordpress.android.fluxc.domain.Addon
import java.math.BigDecimal

class ProductAddonCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.style.Woo_Card
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding: ProductAddonCardBinding =
        ProductAddonCardBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        binding.optionsList.layoutManager =
            LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        binding.optionsList.addItemDecoration(
            AlignedDividerDecoration(
                context,
                DividerItemDecoration.VERTICAL,
                R.id.option_name,
                clipToMargin = false
            )
        )
    }

    fun bind(
        addon: Addon,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        orderMode: Boolean
    ) = with(binding) {
        addonName.text = addon.name
        if (addon is Addon.HasOptions) {
            bindOptionList(addon, formatCurrencyForDisplay)
        }
        if (addon is Addon.HasAdjustablePrice) {
            bindAdjustedPrice(addon, formatCurrencyForDisplay)
        }
        if (orderMode.not()) bindDescription(addon)
    }

    private fun ProductAddonCardBinding.bindAdjustedPrice(
        addon: Addon.HasAdjustablePrice,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        when (val price = addon.price) {
            is Addon.HasAdjustablePrice.Price.Adjusted -> {
                addonCustomPrice.apply {
                    visibility = View.VISIBLE
                    text = price.handlePriceType(formatCurrencyForDisplay)
                }
            }
            Addon.HasAdjustablePrice.Price.NotAdjusted -> {
                addonCustomPrice.visibility = View.GONE
            }
        }
    }

    private fun ProductAddonCardBinding.bindDescription(
        addon: Addon
    ) {
        addonDescription.text = addon.description
        addonDescription.visibility = if (addon.description.isNullOrEmpty()) GONE else VISIBLE
    }

    private fun ProductAddonCardBinding.bindOptionList(
        addon: Addon.HasOptions,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        optionsList.visibility = GONE

        addon.options.takeIf { it.isNotEmpty() }
            ?.let { optionsList.adapter = AddonOptionListAdapter(it, formatCurrencyForDisplay) }
            ?.also { optionsList.visibility = VISIBLE }
    }
}
