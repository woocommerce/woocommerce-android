package com.woocommerce.android.ui.analytics.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardItemViewBinding
import com.woocommerce.android.di.GlideApp

class AnalyticsListCardItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsListCardItemViewBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun setInformation(viewState: AnalyticsListCardItemViewState) {
        binding.analyticsCardListItemTitle.text = viewState.title
        binding.analyticsCardListItemValue.text = viewState.value
        binding.analyticsCardListItemDescription.text = viewState.description
        binding.divider.isVisible = viewState.showDivider == true
        GlideApp
            .with(binding.root.context)
            .applyDefaultRequestOptions(
                RequestOptions
                    .bitmapTransform(RoundedCorners(resources.getDimension(R.dimen.corner_radius_medium).toInt()))
            )
            .load(viewState.imageUri)
            .into(binding.analyticsCardListItemImage)
    }
}
