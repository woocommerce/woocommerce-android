package com.woocommerce.android.ui.analytics.hub.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationCardViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubInformationViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubInformationViewState.NoDataState
import com.woocommerce.android.widgets.SkeletonView

typealias SeeReportClickListener = (url: String) -> Unit

class AnalyticsHubInformationCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()

    var onSeeReportClickListener: SeeReportClickListener? = null

    internal fun updateInformation(viewState: AnalyticsHubInformationViewState) {
        when (viewState) {
            is LoadingViewState -> setSkeleton()
            is DataViewState -> setDataViewState(viewState)
            is NoDataState -> setNoDataViewState(viewState)
        }
    }

    private fun setSkeleton() {
        skeletonView.show(
            binding.analyticsCardDataContainer,
            R.layout.skeleton_analytics_information_card,
            delayed = true
        )
        visibility = View.VISIBLE
    }

    private fun setDataViewState(viewState: DataViewState) {
        skeletonView.hide()
        binding.analyticsCardTitle.text = viewState.title
        binding.leftAnalyticsSection.setViewState(viewState.leftSection)
        binding.rightAnalyticsSection.setViewState(viewState.rightSection)
        binding.analyticsCardTitle.visibility = VISIBLE
        binding.leftAnalyticsSection.visibility = VISIBLE
        binding.rightAnalyticsSection.visibility = VISIBLE
        binding.noDataText.visibility = GONE
        if (viewState.reportUrl != null) {
            binding.reportGroup.visibility = VISIBLE
            binding.reportText.setOnClickListener {
                onSeeReportClickListener?.let { it(viewState.reportUrl) }
            }
        } else {
            binding.reportGroup.visibility = GONE
        }
        visibility = VISIBLE
    }

    private fun setNoDataViewState(viewState: NoDataState) {
        skeletonView.hide()
        binding.noDataText.text = viewState.message
        binding.analyticsCardTitle.visibility = GONE
        binding.leftAnalyticsSection.visibility = GONE
        binding.rightAnalyticsSection.visibility = GONE
        binding.noDataText.visibility = VISIBLE
        visibility = VISIBLE
    }
}
