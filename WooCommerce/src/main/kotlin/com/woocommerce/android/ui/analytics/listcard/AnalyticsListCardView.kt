package com.woocommerce.android.ui.analytics.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardViewBinding
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.*
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import kotlin.math.absoluteValue

class AnalyticsListCardView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsListCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()

    internal fun updateInformation(viewState: AnalyticsListViewState) {
        when (viewState) {
            is LoadingViewState -> setSkeleton()
            is DataViewState -> setDataViewState(viewState)
            is NoDataState -> setNoDataViewState(viewState)
        }
    }

    fun setSeeReportClickListener(onClickListener: (() -> Unit)) {
        binding.seeReportText.setOnClickListener { onClickListener() }
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

    private fun setDataViewState(viewState: DataViewState) {
        val inflater = LayoutInflater.from(context)
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
        binding.analyticsItemsList.removeAllViews()
        viewState.items.forEach { addListItem(inflater, binding.analyticsItemsList, it) }
        binding.analyticsItemsTag.visibility = if (viewState.showDelta) View.VISIBLE else View.GONE
        binding.analyticsCardTitle.visibility = VISIBLE
        binding.analyticsItemsTitle.visibility = VISIBLE
        binding.analyticsItemsValue.visibility = VISIBLE
        binding.analyticsListLeftHeader.visibility = VISIBLE
        binding.analyticsListRightHeader.visibility = VISIBLE
        binding.noDataText.visibility = GONE
    }

    private fun setNoDataViewState(viewState: NoDataState) {
        skeletonView.hide()
        binding.noDataText.text = viewState.message
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsItemsTag.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.noDataText.visibility = VISIBLE
    }

    private fun addListItem(
        inflater: LayoutInflater,
        listContainer: ViewGroup,
        viewState: AnalyticsListCardItemViewState
    ) {
        val listItemView = inflater.inflate(R.layout.analytics_list_card_item, listContainer, false)
            as AnalyticsListCardItemView
        listItemView.cardElevation = resources.getDimension(R.dimen.minor_00)
        listItemView.setInformation(viewState)
        listContainer.addView(listItemView)
    }

    private fun getDeltaTagText(viewState: DataViewState) =
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
            if (delta > 0) ContextCompat.getColor(context, R.color.analytics_delta_positive_color)
            else ContextCompat.getColor(context, R.color.analytics_delta_tag_negative_color)
    }
}
