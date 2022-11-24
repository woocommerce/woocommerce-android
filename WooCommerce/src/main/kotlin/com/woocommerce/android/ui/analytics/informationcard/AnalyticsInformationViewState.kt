package com.woocommerce.android.ui.analytics.informationcard

sealed class AnalyticsInformationViewState {
    object HiddenState : AnalyticsInformationViewState()
    object LoadingViewState : AnalyticsInformationViewState()
    data class NoDataState(val message: String) : AnalyticsInformationViewState()
    data class DataViewState(
        val title: String,
        val leftSection: AnalyticsInformationSectionViewState,
        val rightSection: AnalyticsInformationSectionViewState
    ) : AnalyticsInformationViewState()
}
