package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.ui.analytics.hub.daterangeselector.AnalyticsHubDateRangeSelectorViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class AnalyticsViewState(
    val refreshIndicator: RefreshIndicator,
    val analyticsDateRangeSelectorState: AnalyticsHubDateRangeSelectorViewState,
    val cards: AnalyticsHubCardViewState,
    val ctaState: AnalyticsHubUserCallToActionViewState,
    val showFeedBackBanner: Boolean,
    val lastUpdateTimestamp: String
)

sealed class AnalyticsViewEvent : MultiLiveEvent.Event() {
    data class OpenUrl(val url: String) : AnalyticsViewEvent()
    data class OpenWPComWebView(val url: String) : AnalyticsViewEvent()
    data class OpenDatePicker(val fromMillis: Long, val toMillis: Long) : MultiLiveEvent.Event()
    object OpenDateRangeSelector : AnalyticsViewEvent()
    object SendFeedback : AnalyticsViewEvent()
    object OpenSettings : AnalyticsViewEvent()
    data class OpenGoogleAdsCreation(
        val url: String,
        val isCreationFlow: Boolean,
        val title: String
    ) : AnalyticsViewEvent()
}

sealed class RefreshIndicator {
    object ShowIndicator : RefreshIndicator()
    object NotShowIndicator : RefreshIndicator()
}
