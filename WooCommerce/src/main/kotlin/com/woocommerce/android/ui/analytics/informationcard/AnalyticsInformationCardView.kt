package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsInformationCardViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.*

class AnalyticsInformationCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun updateInformation(viewState: AnalyticsInformationViewState) = when (viewState) {
        is LoadingViewState -> visibility = INVISIBLE
        is DataViewState -> setDataViewState(viewState)
        is NoDataState -> setNoDataViewState(viewState)
    }

    private fun setDataViewState(viewState: DataViewState) {
        binding.tvAnalyticsCardTitle.text = viewState.title
        binding.vSectionTotalSales.setViewState(viewState.totalValues)
        binding.vSectionNetSales.setViewState(viewState.netValues)
        binding.tvAnalyticsCardTitle.visibility = VISIBLE
        binding.llInformationPanel.visibility = VISIBLE
        binding.tvNoData.visibility = GONE
        visibility = VISIBLE
    }

    private fun setNoDataViewState(viewState: NoDataState) {
        binding.tvNoData.text = viewState.message
        binding.tvAnalyticsCardTitle.visibility = GONE
        binding.llInformationPanel.visibility = GONE
        binding.tvNoData.visibility = VISIBLE
        visibility = VISIBLE
    }
}
