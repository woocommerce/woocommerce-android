package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsInformationCardViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.HiddenViewState

class AnalyticsInformationCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun updateInformation(viewState: AnalyticsInformationViewState) {
        when (viewState) {
            is HiddenViewState -> visibility = View.INVISIBLE
            is DataViewState -> {
                visibility = View.VISIBLE
                binding.tvAnalyticsCardTitle.text = viewState.title
                binding.vSectionTotalSales.setViewState(viewState.totalValues)
                binding.vSectionNetSales.setViewState(viewState.netValues)
            }
        }
    }
}
