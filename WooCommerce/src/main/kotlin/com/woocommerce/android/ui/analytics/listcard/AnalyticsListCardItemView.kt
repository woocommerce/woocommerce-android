package com.woocommerce.android.ui.analytics.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsListCardItemViewBinding
import com.woocommerce.android.di.GlideApp

class AnalyticsListCardItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {

    val binding = AnalyticsListCardItemViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun setInformation(viewState: AnalyticsListCardItemViewState) {
        binding.analyticsCardListItemTitle.text = viewState.title
        binding.analyticsCardListItemValue.text = viewState.title
        binding.analyticsCardListItemDescription.text = viewState.description
        GlideApp
            .with(binding.root.context)
            .load(viewState.imageUri)
            .into(binding.analyticsCardListItemImage)
    }
}
