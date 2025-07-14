package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ShippingNoticeBannerBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand

class ShippingNoticeCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = ShippingNoticeBannerBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var onDismissClicked: () -> Unit = {}
    var onLearnMoreClicked: (url: String) -> Unit = {}
    var isVisible: Boolean = visibility == VISIBLE
        set(show) {
            if (show != isVisible) {
                field = show
                if (show) expand() else collapse()
            }
        }
    var message: CharSequence
        get() = binding.message.text
        set(value) = value.let { binding.message.text = it }

    init {
        binding.dismissButton.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.EU_SHIPPING_NOTICE_DISMISSED)
            onDismissClicked()
            AppPrefs.isEUShippingNoticeDismissed = true
            collapse()
        }

        binding.learnMoreButton.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.EU_SHIPPING_NOTICE_LEARN_MORE_TAPPED)
            onLearnMoreClicked(AppUrls.EU_SHIPPING_CUSTOMS_REQUIREMENTS)
        }
    }
}
