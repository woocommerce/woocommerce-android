package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationSectionViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState.SectionDataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState.SectionHiddenViewState
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import kotlin.math.absoluteValue

internal class AnalyticsInformationSectionView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationSectionViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun setViewState(sectionViewState: AnalyticsInformationSectionViewState) {
        when (sectionViewState) {
            is SectionHiddenViewState -> visibility = INVISIBLE
            is SectionDataViewState -> {
                visibility = View.VISIBLE
                binding.tvSectionCardTitle.text = sectionViewState.title
                binding.tvSectionCardValue.text = sectionViewState.value
                binding.tvSectionCardDeltaValue.text =
                    ctx.resources.getString(R.string.analytics_information_card_delta,
                        sectionViewState.sign, sectionViewState.delta)
                binding.tvSectionCardDeltaValue.tag =
                    AnalyticsInformationSectionDeltaTag(sectionViewState.delta, getDeltaTagText(sectionViewState))
            }
        }
    }

    private fun getDeltaTagText(sectionDataViewState: SectionDataViewState) =
        ctx.resources.getString(R.string.analytics_information_card_delta,
            sectionDataViewState.sign,
            sectionDataViewState.delta.absoluteValue)

    class AnalyticsInformationSectionDeltaTag(private val delta: Int, private val text: String) : ITag(text) {
        override fun getTagConfiguration(context: Context): TagConfig {
            val config = TagConfig(context)
                .apply {
                    tagText = text
                    fgColor = ContextCompat.getColor(context, R.color.woo_white)
                    bgColor = getDeltaTagBackgroundColor(context)
                }
            return config
        }

        private fun getDeltaTagBackgroundColor(context: Context) =
            if (delta > 0) ContextCompat.getColor(context, R.color.analytics_delta_positive_color)
            else ContextCompat.getColor(context, R.color.analytics_delta_tag_negative_color)
    }
}
