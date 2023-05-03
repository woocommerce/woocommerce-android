package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.databinding.ShippingNoticeBannerBinding
import com.woocommerce.android.extensions.collapse

class ShippingNoticeCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = ShippingNoticeBannerBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        binding.dismissButton.setOnClickListener {
            AppPrefs.isEUShippingNoticeDismissed = true
            collapse()
        }
    }

    fun setLearnMoreClickListener(action: (url: String) -> Unit) {
        binding.learnMoreButton.setOnClickListener {
            action(AppUrls.EU_SHIPPING_CUSTOMS_REQUIREMENTS)
        }
    }
}
