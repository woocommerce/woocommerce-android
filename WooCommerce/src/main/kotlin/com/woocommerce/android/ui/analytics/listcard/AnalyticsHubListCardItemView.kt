package com.woocommerce.android.ui.analytics.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardItemViewBinding
import com.woocommerce.android.di.GlideApp
import org.wordpress.android.util.PhotonUtils

class AnalyticsHubListCardItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val imageCornerRadius = resources.getDimension(R.dimen.corner_radius_medium).toInt()

    val binding = AnalyticsListCardItemViewBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun setInformation(viewState: AnalyticsHubListCardItemViewState) {
        binding.analyticsCardListItemTitle.text = viewState.title
        binding.analyticsCardListItemValue.text = viewState.value
        binding.analyticsCardListItemDescription.text = viewState.description
        binding.divider.isVisible = viewState.showDivider == true

        GlideApp
            .with(binding.root.context)
            .load(PhotonUtils.getPhotonImageUrl(viewState.imageUri, imageSize, imageSize))
            .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
            .placeholder(R.drawable.ic_product)
            .into(binding.analyticsCardListItemImage)
    }
}
