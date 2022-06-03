package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.OrderDetailInstallWcShippingBannerBinding
import com.woocommerce.android.extensions.collapse

class OrderDetailInstallWcShippingBanner @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailInstallWcShippingBannerBinding.inflate(LayoutInflater.from(ctx), this)

    fun setClickListeners(onInstallWcShipping: () -> Unit, onDismiss: () -> Unit) {
        binding.installWcShippingButton.setOnClickListener { onInstallWcShipping() }
        binding.dismissShippingLabelBannerButton.setOnClickListener {
            this.collapse(duration = 200L)
            onDismiss()
        }
    }
}
