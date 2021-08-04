package com.woocommerce.android.ui.products.addons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding: ProductAddonCardBinding =
        ProductAddonCardBinding.inflate(
            LayoutInflater.from(context),
            this
        )

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        binding.optionsList.layoutManager =
            LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        binding.optionsList.addItemDecoration(AlignedDividerDecoration(
            context,
            DividerItemDecoration.VERTICAL,
            R.id.option_name,
            clipToMargin = false
        ))
    }

    fun bind(
        addon: ProductAddon,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) = with(binding) {
        name.text = addon.name
        description.text = addon.description
        optionsList.adapter = AddonOptionListAdapter(addon.options, formatCurrencyForDisplay)
    }
}
