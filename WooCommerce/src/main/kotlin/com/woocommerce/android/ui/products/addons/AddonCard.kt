package com.woocommerce.android.ui.products.addons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.woocommerce.android.databinding.AddonItemBinding
import com.woocommerce.android.model.ProductAddon

class AddonCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding =
        AddonItemBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

    fun bind(addon: ProductAddon) {
        binding.addonName.text = addon.name
    }
}
