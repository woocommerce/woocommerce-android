package com.woocommerce.android.ui.products.propertyviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.ProductPropertyLinkViewLayoutBinding

class WCProductPropertyLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val binding: ProductPropertyLinkViewLayoutBinding = ProductPropertyLinkViewLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    fun show(
        title: String,
        icon: Int?,
        showDivider: Boolean
    ) {
        binding.text.text = title

        if (icon != null) {
            binding.icon.isVisible = true
            binding.icon.setImageResource(icon)
        } else {
            binding.icon.isVisible = false
        }

        binding.divider.isVisible = showDivider
    }
}
