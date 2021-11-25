package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsCardInformationContract.AnalyticsCardInformationViewState

class AnalyticsContract {
    data class AnalyticsState(val analyticsDateRangeSelectorState: AnalyticsDateRangeSelectorViewState,
                              val revenueCardState: AnalyticsCardInformationViewState)

    sealed class AnalyticsEvent
    sealed class AnalyticsEffect
}


