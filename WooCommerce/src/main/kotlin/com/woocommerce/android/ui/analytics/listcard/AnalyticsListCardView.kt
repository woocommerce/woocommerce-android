package com.woocommerce.android.ui.analytics.listcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsListCardViewBinding
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.*
import com.woocommerce.android.widgets.SkeletonView

class AnalyticsListCardView @JvmOverloads constructor(
    ctx: Context,
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
            R.layout.skeleton_analytics_information_card,
            delayed = true
        )
        binding.analyticsCardTitle.visibility = GONE
        binding.analyticsItemsTitle.visibility = GONE
        binding.analyticsItemsValue.visibility = GONE
        binding.analyticsListLeftHeader.visibility = GONE
        binding.analyticsListRightHeader.visibility = GONE
        binding.noDataText.visibility = GONE
    }

    private fun setDataViewState(viewState: DataViewState) {
        skeletonView.hide()
        binding.analyticsCardTitle.text = viewState.title
        binding.analyticsItemsTitle.text = viewState.subTitle
        binding.analyticsItemsValue.text = viewState.subTitleValue
        binding.analyticsListLeftHeader.text = viewState.listLeftHeader
        binding.analyticsListRightHeader.text = viewState.listRightHeader
        val inflater = LayoutInflater.from(context)
        viewState.items.forEach { addListItem(inflater, it) }
        binding.analyticsCardTitle.visibility = VISIBLE
        binding.analyticsItemsTitle.visibility = VISIBLE
        binding.analyticsItemsValue.visibility = VISIBLE
        binding.analyticsItemsTag.visibility = VISIBLE
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

    private fun addListItem(inflater: LayoutInflater, viewState: AnalyticsListCardItemViewState) {
        val listItemView: AnalyticsListCardItemView =
            inflater.inflate(R.layout.analytics_list_card_item_view, this, true)
                as AnalyticsListCardItemView
        listItemView.setInformation(viewState)
        addView(listItemView)
    }
}
