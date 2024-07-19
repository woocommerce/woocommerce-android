package com.woocommerce.android.ui.analytics.hub.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardItemViewBinding
import com.woocommerce.android.util.StringUtils
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

        contentDescription = getViewContentDescription(
            context = context,
            title = viewState.title,
            description = viewState.description,
            value = viewState.value
        )

        if (viewState.showImage) {
            Glide.with(binding.root.context)
                .load(PhotonUtils.getPhotonImageUrl(viewState.imageUri, imageSize, imageSize))
                .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                .placeholder(R.drawable.ic_product)
                .into(binding.analyticsCardListItemImage)
        } else {
            binding.analyticsCardListItemImage.visibility = GONE
        }
    }

    private fun getViewContentDescription(context: Context, title: String, description: String, value: String): String {
        val items = StringUtils.getQuantityString(
            context = context,
            quantity = value.toIntOrNull() ?: 1,
            one = R.string.analytics_item,
            default = R.string.analytics_items
        )
        return context.getString(R.string.analytics_list_item_products_sold, title, description, value, items)
    }
}
