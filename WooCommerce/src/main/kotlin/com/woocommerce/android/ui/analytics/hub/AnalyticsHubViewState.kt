package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.ui.analytics.hub.daterangeselector.AnalyticsHubDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class AnalyticsViewState(
    val refreshIndicator: RefreshIndicator,
    val analyticsDateRangeSelectorState: AnalyticsHubDateRangeSelectorViewState,
    val revenueState: AnalyticsHubInformationViewState,
    val ordersState: AnalyticsHubInformationViewState,
    val productsState: AnalyticsHubListViewState,
    val sessionState: AnalyticsHubInformationViewState,
    val showFeedBackBanner: Boolean
)

sealed class AnalyticsViewEvent : MultiLiveEvent.Event() {
    data class OpenUrl(val url: String) : AnalyticsViewEvent()
    data class OpenWPComWebView(val url: String) : AnalyticsViewEvent()
    data class OpenDatePicker(val fromMillis: Long, val toMillis: Long) : MultiLiveEvent.Event()
    object OpenDateRangeSelector : AnalyticsViewEvent()
    object SendFeedback : AnalyticsViewEvent()
}

sealed class RefreshIndicator {
    object ShowIndicator : RefreshIndicator()
    object NotShowIndicator : RefreshIndicator()
}
