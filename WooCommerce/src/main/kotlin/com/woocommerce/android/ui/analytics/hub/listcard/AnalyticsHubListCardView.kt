package com.woocommerce.android.ui.analytics.hub.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubListViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.SeeReportClickListener
import com.woocommerce.android.ui.analytics.hub.toReportCard
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import kotlin.math.absoluteValue

class AnalyticsHubListCardView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsListCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()
    var onSeeReportClickListener: SeeReportClickListener? = null

    internal fun updateInformation(viewState: AnalyticsHubListViewState) {
        when (viewState) {
            is AnalyticsHubListViewState.LoadingViewState -> setSkeleton()
            is AnalyticsHubListViewState.DataViewState -> setDataViewState(viewState)
            is AnalyticsHubListViewState.NoDataState -> setNoDataViewState(viewState)
        }
    }

    private fun setSkeleton() {
        skeletonView.show(
            binding.analyticsCardListContainer,
            R.layout.skeleton_analytics_list_card,
            delayed = true
        )
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.analyticsItemsTag.visibility = View.GONE
        binding.noDataText.visibility = GONE
    }

    private fun setDataViewState(viewState: AnalyticsHubListViewState.DataViewState) {
        skeletonView.hide()
        binding.analyticsCardTitle.text = viewState.title
        binding.analyticsItemsTitle.text = viewState.subTitle
        binding.analyticsItemsValue.text = viewState.subTitleValue
        binding.analyticsListLeftHeader.text = viewState.listLeftHeader
        binding.analyticsListRightHeader.text = viewState.listRightHeader
        binding.analyticsItemsTag.text = ctx.resources.getString(
            R.string.analytics_information_card_delta,
            viewState.sign, viewState.delta
        )
        viewState.delta?.let {
            binding.analyticsItemsTag.tag = AnalyticsListDeltaTag(viewState.delta, getDeltaTagText(viewState))
        }
        binding.analyticsItemsList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = AnalyticsHubListAdapter(viewState.items)
            visibility = VISIBLE
        }
        binding.analyticsItemsTag.isVisible = viewState.delta != null
        binding.analyticsCardTitle.visibility = VISIBLE
        binding.analyticsItemsTitle.visibility = VISIBLE
        binding.analyticsItemsValue.visibility = VISIBLE
        binding.analyticsListLeftHeader.visibility = VISIBLE
        binding.analyticsListRightHeader.visibility = VISIBLE
        binding.noDataText.visibility = GONE
        if (viewState.reportUrl != null) {
            binding.reportGroup.visibility = VISIBLE
            binding.reportText.setOnClickListener {
                onSeeReportClickListener?.let {
                    val card = viewState.card.toReportCard()
                    if (card != null) it(viewState.reportUrl, card)
                }
            }
        } else {
            binding.reportGroup.visibility = GONE
        }
    }

    private fun setNoDataViewState(viewState: AnalyticsHubListViewState.NoDataState) {
        skeletonView.hide()
        binding.noDataText.text = viewState.message
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsItemsTag.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.noDataText.visibility = VISIBLE
        binding.reportGroup.visibility = GONE
        binding.analyticsItemsList.visibility = GONE
    }

    private fun getDeltaTagText(viewState: AnalyticsHubListViewState.DataViewState) =
        ctx.resources.getString(
            R.string.analytics_information_card_delta,
            viewState.sign,
            viewState.delta?.absoluteValue
        )

    class AnalyticsListDeltaTag(private val delta: Int, private val text: String) : ITag(text) {
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
