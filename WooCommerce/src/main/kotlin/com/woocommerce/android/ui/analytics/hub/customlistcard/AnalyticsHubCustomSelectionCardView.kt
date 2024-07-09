package com.woocommerce.android.ui.analytics.hub.customlistcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsCustomSelectionCardViewBinding

class AnalyticsHubCustomSelectionCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsCustomSelectionCardViewBinding.inflate(LayoutInflater.from(ctx), this)
}
