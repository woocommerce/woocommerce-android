package com.woocommerce.android.ui.analytics.informationcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsInformationCardViewBinding
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsCardInformationContract.AnalyticsCardInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsCardInformationContract.AnalyticsCardInformationViewState.CardDataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsCardInformationContract.AnalyticsCardInformationViewState.HiddenCardViewState

class AnalyticsInformationCardView @JvmOverloads constructor(
    val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsInformationCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun setViewState(viewState: AnalyticsCardInformationViewState) {

        when (viewState) {
            is HiddenCardViewState -> visibility = View.INVISIBLE
            is CardDataViewState -> {
                binding.tvAnalyticsCardTitle.text = viewState.title
                binding.vSectionTotalSales.setViewState(viewState.totalValues)
                binding.vSectionNetSales.setViewState(viewState.netValues)
            }
        }
    }
}
