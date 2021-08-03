package com.woocommerce.android.ui.products.addons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductAddonCardBinding
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.addons.options.AddonOptionListAdapter

class ProductAddonCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
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
    }

    fun bind(addon: ProductAddon) = with(binding) {
        name.text = addon.name
        description.text = addon.description
        optionsList.adapter = AddonOptionListAdapter(addon.options)
    }
}
