package com.woocommerce.android.ui.analytics.hub.customlistcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsCustomSelectionCardViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.LoadingAdsViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.NoAdsState
import com.woocommerce.android.ui.analytics.hub.informationcard.SeeReportClickListener
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListAdapter
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListCardView
import com.woocommerce.android.ui.analytics.hub.toReportCard
import com.woocommerce.android.widgets.SkeletonView
import kotlin.math.absoluteValue

class AnalyticsHubCustomSelectionCardView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsCustomSelectionCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()
    var onSeeReportClickListener: SeeReportClickListener? = null

    fun updateInformation(viewState: AnalyticsHubCustomSelectionListViewState) {
        when (viewState) {
            is LoadingAdsViewState -> setSkeleton()
            is NoAdsState -> setNoAdsViewState(viewState)
            is DataViewState -> setDataViewState(viewState)
        }
    }

    private fun setDataViewState(viewState: DataViewState) {
        skeletonView.hide()
        binding.analyticsCardTitle.text = viewState.title
        binding.analyticsItemsTitle.text = viewState.subTitle
        binding.analyticsItemsValue.text = viewState.itemTitleValue
        binding.analyticsListLeftHeader.text = viewState.listLeftHeader
        binding.analyticsListRightHeader.text = viewState.listRightHeader
        binding.analyticsItemsTag.text = ctx.resources.getString(
            R.string.analytics_information_card_delta,
            viewState.sign, viewState.delta
        )
        viewState.delta?.let {
            binding.analyticsItemsTag.tag =
                AnalyticsHubListCardView.AnalyticsListDeltaTag(viewState.delta, getDeltaTagText(viewState))
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

        viewState.reportUrl?.let {
            binding.reportGroup.visibility = VISIBLE
            binding.reportText.setOnClickListener {
                onSeeReportClickListener?.let {
                    val card = viewState.card.toReportCard()
                    if (card != null) it(viewState.reportUrl, card)
                }
            }
        } ?: run { binding.reportGroup.visibility = GONE }

        if (viewState.filterOptions.isNotEmpty()) {
            with(binding.analyticsFilterButton) {
                visibility = VISIBLE
                setOnClickListener { it.displayFilterPopupMenu(viewState) }
            }
        } else {
            binding.analyticsFilterButton.visibility = GONE
        }
    }

    private fun View.displayFilterPopupMenu(
        viewState: DataViewState
    ) {
        PopupMenu(ctx, this).apply {
            viewState.filterOptions.forEach { menu.add(it) }
            setOnMenuItemClickListener { item ->
                viewState.onFilterSelected(item.title.toString())
                true
            }
        }.show()
    }

    private fun setNoAdsViewState(viewState: NoAdsState) {
        skeletonView.hide()
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.analyticsItemsTag.visibility = GONE
        binding.analyticsFilterButton.visibility = GONE
        binding.noDataText.visibility = VISIBLE
        binding.noDataText.text = viewState.message
    }

    private fun setSkeleton() {
        skeletonView.show(
            binding.analyticsCustomCardListContainer,
            R.layout.skeleton_analytics_list_card,
            delayed = true
        )
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.analyticsItemsTag.visibility = GONE
        binding.noDataText.visibility = GONE
        binding.analyticsFilterButton.visibility = GONE
    }

    private fun getDeltaTagText(viewState: DataViewState) =
        ctx.resources.getString(
            R.string.analytics_information_card_delta,
            viewState.sign,
            viewState.delta?.absoluteValue
        )
}
