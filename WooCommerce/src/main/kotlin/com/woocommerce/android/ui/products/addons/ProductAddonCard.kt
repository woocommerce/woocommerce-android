package com.woocommerce.android.ui.products.addons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductAddonCardBinding
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter
import com.woocommerce.android.widgets.AlignedDividerDecoration
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
        addon: ProductAddon,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) = with(binding) {
        addonName.text = addon.name
        bindDescription(addon)
        bindOptionList(addon, formatCurrencyForDisplay)
    }

    private fun ProductAddonCardBinding.bindDescription(
        addon: ProductAddon
    ) {
        addonDescription.text = addon.description
        addonDescription.visibility = if (addon.description.isNotEmpty()) VISIBLE else GONE
    }

    private fun ProductAddonCardBinding.bindOptionList(
        addon: ProductAddon,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        optionsList.visibility = GONE
        addon.takeIf { it.rawOptions.isNotEmpty() }
            ?.options
            ?.let { optionsList.adapter = AddonOptionListAdapter(it, formatCurrencyForDisplay) }
            ?.also { optionsList.visibility = VISIBLE }
    }
}
