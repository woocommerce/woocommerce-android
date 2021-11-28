package com.woocommerce.android.ui.analytics.informationcard

sealed class AnalyticsCardInformationViewState {
    object HiddenViewState : AnalyticsCardInformationViewState()
    data class DataViewState(val title: String,
                             val totalValues: SectionViewState,
                             val netValues: SectionViewState)
        : AnalyticsCardInformationViewState()
}
