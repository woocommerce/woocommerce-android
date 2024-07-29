package com.woocommerce.android.ui.analytics.hub.cta

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsCallToActionViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubUserCallToActionViewState

class AnalyticsHubUserCallToActionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsCallToActionViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateInformation(viewState: AnalyticsHubUserCallToActionViewState) {
        if (viewState.isVisible) {
            binding.ctaTitle.text = viewState.title
            binding.ctaDescription.text = viewState.description
            binding.buttonCtaAction.text = viewState.callToActionText
            binding.buttonCtaAction.setOnClickListener {
                viewState.onCallToActionClickListener()
            }
            binding.root.visibility = VISIBLE
        } else {
            binding.root.visibility = GONE
        }
    }
}
