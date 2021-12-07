package com.woocommerce.android.ui.analytics.informationcard

sealed class AnalyticsInformationSectionViewState {
    object SectionHiddenViewState : AnalyticsInformationSectionViewState()
    data class SectionDataViewState(
        val title: String,
        val value: String,
        val delta: Int
    ) : AnalyticsInformationSectionViewState() {
        val sign: String
            get() = when {
                delta > 0 -> "+"
                delta == 0 -> ""
                else -> "-"
            }
    }
}
