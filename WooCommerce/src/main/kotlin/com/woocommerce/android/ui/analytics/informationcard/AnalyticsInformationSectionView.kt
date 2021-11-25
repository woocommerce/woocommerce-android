package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationSectionViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionContract.ViewState
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig

class AnalyticsInformationSectionView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationSectionViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun initView(viewState: ViewState) {
        binding.tvSectionCardTitle.text = viewState.title
        binding.tvSectionCardValue.text = viewState.value
        binding.tvSectionCardDeltaValue.text =
            ctx.resources.getString(R.string.analytics_information_card_delta, viewState.getSign(), viewState.delta)
        binding.tvSectionCardDeltaValue.tag =
            AnalyticsInformationSectionDeltaTag(viewState.delta, getDeltaTagText(viewState)
        )
    }

    private fun getDeltaTagText(viewState: ViewState) =
        ctx.resources.getString(R.string.analytics_information_card_delta, viewState.getSign(), viewState.delta)
}

class AnalyticsInformationSectionDeltaTag(private val delta: Int, private val text: String) : ITag(text) {
    override fun getTagConfiguration(context: Context): TagConfig {
        val config = TagConfig(context)
            .apply {
                tagText = text
                bgColor = getDeltaTagBackgroundColor(context)
            }
        return config
    }

    private fun getDeltaTagBackgroundColor(context: Context) =
        if (delta > 0) ContextCompat.getColor(context, R.color.analytics_delta_positive_color)
        else ContextCompat.getColor(context, R.color.analytics_delta_tag_negative_color)
}
