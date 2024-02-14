package com.woocommerce.android.ui.analytics.hub.listcard

sealed class AnalyticsHubListViewState {
    object LoadingViewState : AnalyticsHubListViewState()
    data class NoDataState(val message: String) : AnalyticsHubListViewState()
    data class DataViewState(
        val title: String,
        val subTitle: String,
        val subTitleValue: String,
        val delta: Int?,
        val listLeftHeader: String,
        val listRightHeader: String,
        val items: List<AnalyticsHubListCardItemViewState>,
        val reportUrl: String?
    ) : AnalyticsHubListViewState() {
        val sign: String
            get() = when {
                delta == null -> ""
                delta == 0 -> ""
                delta > 0 -> "+"
                else -> "-"
            }
    }
}
