package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.OrderDetailInstallWcShippingBannerBinding

class OrderDetailInstallWcShippingBanner @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailInstallWcShippingBannerBinding.inflate(LayoutInflater.from(ctx), this)

    fun setOnDismissListener(onDismiss: () -> Unit) {
        binding.dismissShippingLabelBannerButton.setOnClickListener {
            onDismiss()
        }
    }
}
