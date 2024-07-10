package com.woocommerce.android.ui.analytics.hub.customlistcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsCustomSelectionCardViewBinding
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.LoadingAdsViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCustomSelectionListViewState.NoAdsState

class AnalyticsHubCustomSelectionCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsCustomSelectionCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateInformation(viewState: AnalyticsHubCustomSelectionListViewState) {
        when (viewState) {
            is LoadingAdsViewState -> setSkeleton()
            is NoAdsState -> setNoAdsViewState(viewState)
            is DataViewState -> setDataViewState(viewState)
        }
    }

    private fun setDataViewState(viewState: DataViewState) {
        TODO("Not yet implemented")
    }

    private fun setNoAdsViewState(viewState: NoAdsState) {
        TODO("Not yet implemented")
    }

    private fun setSkeleton() {
        TODO("Not yet implemented")
    }
}
