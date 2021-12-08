package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationSectionViewBinding
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
        visibility = View.VISIBLE
        binding.cardInformationSectionTitle.text = sectionViewState.title
        binding.cardInformationSectionValue.text = sectionViewState.value
        binding.cardInformationSectionDeltaTag.text =
            ctx.resources.getString(
                R.string.analytics_information_card_delta,
                sectionViewState.sign, sectionViewState.delta
            )
        binding.cardInformationSectionDeltaTag.tag =
            AnalyticsInformationSectionDeltaTag(sectionViewState.delta, getDeltaTagText(sectionViewState))
    }

    private fun getDeltaTagText(sectionDataViewState: AnalyticsInformationSectionViewState) =
        ctx.resources.getString(
            R.string.analytics_information_card_delta,
            sectionDataViewState.sign,
            sectionDataViewState.delta.absoluteValue
        )

    class AnalyticsInformationSectionDeltaTag(private val delta: Int, private val text: String) : ITag(text) {
        override fun getTagConfiguration(context: Context): TagConfig {
            val config = TagConfig(context)
                .apply {
                    tagText = text
                    fgColor = ContextCompat.getColor(context, R.color.analytics_delta_text_color)
                    bgColor = getDeltaTagBackgroundColor(context)
                }
            return config
        }

        private fun getDeltaTagBackgroundColor(context: Context) =
            if (delta > 0) ContextCompat.getColor(context, R.color.analytics_delta_positive_color)
            else ContextCompat.getColor(context, R.color.analytics_delta_tag_negative_color)
    }
}
