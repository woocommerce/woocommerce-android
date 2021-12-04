package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState

data class AnalyticsViewState(val analyticsDateRangeSelectorState: AnalyticsDateRangeSelectorViewState,
                              val revenueState: AnalyticsInformationViewState)
