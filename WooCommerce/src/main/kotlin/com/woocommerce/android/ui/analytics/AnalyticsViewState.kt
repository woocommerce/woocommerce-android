package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class AnalyticsViewState(
    val refreshIndicator: RefreshIndicator,
    val analyticsDateRangeSelectorState: AnalyticsDateRangeSelectorViewState,
    val revenueState: AnalyticsInformationViewState,
    val ordersState: AnalyticsInformationViewState,
    val productsState: AnalyticsListViewState,
    val visitorsState: AnalyticsInformationViewState,
)

sealed class AnalyticsViewEvent : MultiLiveEvent.Event() {
    data class OpenUrl(val url: String) : AnalyticsViewEvent()
    data class OpenWPComWebView(val url: String) : AnalyticsViewEvent()
    data class OpenDatePicker(val fromMillis: Long, val toMillis: Long) : MultiLiveEvent.Event()
    object OpenDateRangeSelector : AnalyticsViewEvent()
}

sealed class RefreshIndicator {
    object ShowIndicator : RefreshIndicator()
    object NotShowIndicator : RefreshIndicator()
}
