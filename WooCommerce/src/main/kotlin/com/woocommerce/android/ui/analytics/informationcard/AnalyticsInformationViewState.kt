package com.woocommerce.android.ui.analytics.informationcard

sealed class AnalyticsInformationViewState {
    object HiddenViewState : AnalyticsInformationViewState()
    data class DataViewState(val title: String,
                             val totalValues: AnalyticsInformationSectionViewState,
                             val netValues: AnalyticsInformationSectionViewState)
        : AnalyticsInformationViewState()
}
