package com.woocommerce.android.ui.analytics.hub.informationcard

sealed class AnalyticsHubInformationViewState {
    object HiddenState : AnalyticsHubInformationViewState()
    object LoadingViewState : AnalyticsHubInformationViewState()
    data class NoDataState(val message: String) : AnalyticsHubInformationViewState()
    data class DataViewState(
        val title: String,
        val leftSection: AnalyticsHubInformationSectionViewState,
        val rightSection: AnalyticsHubInformationSectionViewState,
        val reportUrl: String?
    ) : AnalyticsHubInformationViewState()
}
