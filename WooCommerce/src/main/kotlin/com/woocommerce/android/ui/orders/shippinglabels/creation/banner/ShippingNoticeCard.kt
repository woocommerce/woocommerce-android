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

    var onDismissClicked: () -> Unit = {}
    var onLearnMoreClicked: (url: String) -> Unit = {}

    init {
        binding.dismissButton.setOnClickListener {
            onDismissClicked()
            AppPrefs.isEUShippingNoticeDismissed = true
            collapse()
        }

        binding.learnMoreButton.setOnClickListener {
            onLearnMoreClicked(AppUrls.EU_SHIPPING_CUSTOMS_REQUIREMENTS)
        }
    }
}
