package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsCardInformationContract.AnalyticsCardInformationViewState

class AnalyticsContract {
    data class AnalyticsState(val analyticsDateRangeSelectorState: AnalyticsDateRangeSelectorViewState,
                              val revenueCardState: AnalyticsCardInformationViewState)

}
