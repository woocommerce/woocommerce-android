package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsInformationCardViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.*
import com.woocommerce.android.widgets.SkeletonView

class AnalyticsInformationCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {

    val binding = AnalyticsInformationCardViewBinding.inflate(LayoutInflater.from(ctx), this)
    private var skeletonView = SkeletonView()

    internal fun updateInformation(viewState: AnalyticsInformationViewState) = when (viewState) {
        is LoadingViewState -> setSkeleton()
        is DataViewState -> setDataViewState(viewState)
        is NoDataState -> setNoDataViewState(viewState)
    }

    private fun setSkeleton() {
        skeletonView.show(
            binding.containerAnalyticsCardData,
            R.layout.skeleton_analytics_information_card,
            delayed = true
        )
        visibility = View.VISIBLE
    }

    fun setSeeReportClickListener(onClickListener: ((view: View) -> Unit)) {
        binding.tvSeeReport.setOnClickListener(onClickListener)
    }

    private fun setDataViewState(viewState: DataViewState) {
        skeletonView.hide()
        binding.tvAnalyticsCardTitle.text = viewState.title
        binding.vSectionTotalSales.setViewState(viewState.leftSection)
        binding.vSectionNetSales.setViewState(viewState.rightSection)
        binding.tvAnalyticsCardTitle.visibility = VISIBLE
        binding.llInformationPanel.visibility = VISIBLE
        binding.tvNoData.visibility = GONE
        visibility = VISIBLE
    }

    private fun setNoDataViewState(viewState: NoDataState) {
        skeletonView.hide()
        binding.tvNoData.text = viewState.message
        binding.tvAnalyticsCardTitle.visibility = GONE
        binding.llInformationPanel.visibility = GONE
        binding.tvNoData.visibility = VISIBLE
        visibility = VISIBLE
    }
}
