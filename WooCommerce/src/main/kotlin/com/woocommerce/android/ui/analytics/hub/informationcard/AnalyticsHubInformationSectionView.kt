package com.woocommerce.android.ui.analytics.hub.informationcard

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationSectionViewBinding
import com.woocommerce.android.extensions.greaterThan
import com.woocommerce.android.ui.analytics.hub.LineChart
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import org.wordpress.android.util.DisplayUtils
import kotlin.math.absoluteValue

internal class AnalyticsHubInformationSectionView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationSectionViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun setViewState(sectionViewState: AnalyticsHubInformationSectionViewState) {
        // on devices with a larger font size the revenue value may wrap to multiple lines, so we set the
        // auto sizing programmatically on API levels that support it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val minSize = resources.getDimensionPixelSize(R.dimen.text_minor_100)
            val maxSize = resources.getDimensionPixelSize(R.dimen.text_major_50)
            val granularity = DisplayUtils.spToPx(context, 1F).toInt()
            binding.cardInformationSectionValue.maxLines = 1
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.cardInformationSectionValue,
                minSize,
                maxSize,
                granularity,
                TypedValue.COMPLEX_UNIT_PX
            )
        }

        visibility = View.VISIBLE
        binding.cardInformationSectionTitle.text = sectionViewState.title
        binding.cardInformationSectionValue.text = sectionViewState.value
        binding.cardInformationSectionDeltaTag.text =
            ctx.resources.getString(
                R.string.analytics_information_card_delta,
                sectionViewState.sign, sectionViewState.delta
            )
        sectionViewState.delta?.let {
            binding.cardInformationSectionDeltaTag.tag =
                AnalyticsInformationSectionDeltaTag(it, getDeltaTagText(sectionViewState.sign, it))
        }
        val isDeltaNotNull = sectionViewState.delta != null
        binding.cardInformationSectionDeltaTag.isVisible = isDeltaNotNull
        binding.cardInformationChart.apply {
            isVisible = isDeltaNotNull
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val color = if (sectionViewState.delta.greaterThan(0)) {
                    colorResource(R.color.analytics_delta_positive_color)
                } else {
                    colorResource(R.color.analytics_delta_tag_negative_color)
                }
                WooTheme {
                    LineChart(
                        info = sectionViewState.chartInfo,
                        color = color
                    )
                }
            }
        }
    }

    private fun getDeltaTagText(sign: String, value: Int) =
        ctx.resources.getString(R.string.analytics_information_card_delta, sign, value.absoluteValue)

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
            if (delta > 0) {
                ContextCompat.getColor(context, R.color.analytics_delta_positive_color)
            } else {
                ContextCompat.getColor(context, R.color.analytics_delta_tag_negative_color)
            }
    }
}
