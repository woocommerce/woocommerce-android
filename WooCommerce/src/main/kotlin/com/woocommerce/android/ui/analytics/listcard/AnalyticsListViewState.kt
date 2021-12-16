package com.woocommerce.android.ui.analytics.listcard

sealed class AnalyticsListViewState {
    object LoadingViewState : AnalyticsListViewState()
    data class NoDataState(val message: String) : AnalyticsListViewState()
    data class DataViewState(
        val title: String,
        val subTitle: String,
        val subTitleValue: String,
        val delta: Int?,
        val listLeftHeader: String,
        val listRightHeader: String,
        val items: List<AnalyticsListCardItemViewState>
    ) : AnalyticsListViewState() {
        val sign: String
            get() = when {
                delta == null -> ""
                delta == 0 -> ""
                delta > 0 -> "+"
                else -> "-"
            }
    }
}
