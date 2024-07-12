package com.woocommerce.android.ui.analytics.hub.customlistcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsCustomSelectionCardViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.LoadingAdsViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.NoAdsState
import com.woocommerce.android.widgets.SkeletonView

class AnalyticsHubCustomSelectionCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsCustomSelectionCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()

    fun updateInformation(viewState: AnalyticsHubCustomSelectionListViewState) {
        when (viewState) {
            is LoadingAdsViewState -> setSkeleton()
            is NoAdsState -> setNoAdsViewState(viewState)
            is DataViewState -> setDataViewState(viewState)
        }
    }

    private fun setDataViewState(viewState: DataViewState) {
        viewState.apply { }
    }

    private fun setNoAdsViewState(viewState: NoAdsState) {
        viewState.apply { }
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
}
